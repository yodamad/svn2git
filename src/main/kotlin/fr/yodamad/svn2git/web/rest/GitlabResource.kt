package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.domain.GitlabInfo
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.service.client.GitlabAdmin
import fr.yodamad.svn2git.service.util.STARS
import org.apache.commons.lang3.StringUtils
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.UserApi
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import org.gitlab4j.api.models.User
import org.gitlab4j.api.models.Visibility
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.stream.IntStream

/**
 * Controller to use Gitlab API
 */
@RestController
@RequestMapping("/api/gitlab/")
open class GitlabResource(val gitlabAdmin: GitlabAdmin,
                          val applicationProperties: ApplicationProperties) {

    val logger: Logger = LoggerFactory.getLogger(GitlabResource::class.java)

    /**
     * Check if a user exists on Gitlab
     *
     * @param userName User ID search
     * @return if user found
     */
    @Timed
    @PostMapping("user/{username}")
    open fun checkUser(@PathVariable("username") userName: String?, @RequestBody gitlabInfo: GitlabInfo): ResponseEntity<Boolean?>? {
        val api: UserApi = overrideGitlab(gitlabInfo).userApi
        val user = api.getOptionalUser(userName)
        val exception = GitLabApi.getOptionalException(user)
        if (exception != null) {
            logger.error("Fail to access gitlab", exception)
            return ResponseEntity.badRequest().build()
        }
        return if (user.isPresent) {
            ResponseEntity.ok().body(user.isPresent)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Check if a group exists on Gitlab, AND that the passed in userName is a member of that group
     *
     * @param groupName Group name search
     * @param userName  Member of group we are checking for
     * @return if group found
     */
    @Timed
    @PostMapping("group/{groupName}/members/{userName}")
    open fun checkGroup(@PathVariable("groupName") groupName: String?,
                        @PathVariable("userName") userName: String?,
                        @RequestBody gitlabInfo: GitlabInfo): ResponseEntity<Boolean>? {
        val gitlab = overrideGitlab(gitlabInfo)
        // Group where project will be created
        val group = gitlab.groupApi.getOptionalGroup(groupName)

        // User that will be used to create a project
        var user: Optional<User>?
        // if token is blank / emtpy, we use the default gitlab user
        user = if (StringUtils.isBlank(gitlabInfo.token)) {
            gitlab.userApi.getOptionalUser(applicationProperties.gitlab.account)
        } else {
            // Username is the username that is checked in first step of wizard
            gitlab.userApi.getOptionalUser(userName)
        }

        // if the group is present and the user that will use it is present
        return if (group.isPresent && user.isPresent) {
            try {

                // check if userName passed in is a member of the group (includingInherited)
                // An exception is thrown if member is not found in group.
                val member = gitlab.groupApi.getMember(group.get().id, user.get().id)
                if (member != null) {
                    logger.info(String.format("User:%s is a member of the target group:%s", user.get().username, group.get().name))

                    // Group exists and the passed in username is a member of it
                    ResponseEntity.ok().body(group.isPresent)
                } else {
                    // This should not be possible
                    logger.error(String.format("Member:%s not found in group:%s",
                        user.get().username, group.get().name))

                    // Group exists but an unexpected error getting user
                    ResponseEntity.notFound().build()
                }
            } catch (apiEx: GitLabApiException) {

                // just in case token in message
                val message = apiEx.message!!.replace(applicationProperties.gitlab.token, STARS)
                if (apiEx.reason.equals("Not Found", ignoreCase = true)) {
                    // UserName not found : is a possible case. i.e. we only raise a warning in logs.
                    logger.warn(String.format("Member:%s not found in group:%s",
                        user.get().username, group.get().name))
                } else {
                    logger.error("Error getting member of group", message)
                }

                // User didn't exist in group
                ResponseEntity.notFound().build()
            }
        } else {
            // Group didn't exist or user didn't exist
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Creates a group on Gitlab
     *
     * @param groupName Group name to create
     * @return
     */
    @Timed
    @PutMapping("group/{groupName}")
    open fun createGroup(@PathVariable("groupName") groupName: String?, @RequestBody gitlabInfo: GitlabInfo?): ResponseEntity<Boolean>? {
        val group = Group()
        group.name = groupName
        group.path = groupName
        group.visibility = Visibility.INTERNAL
        return try {
            overrideGitlab(gitlabInfo!!).groupApi.addGroup(group)
            ResponseEntity.ok().body(true)
        } catch (apiEx: GitLabApiException) {
            ResponseEntity.badRequest().build()
        }
    }

    /**
     * Override Gitlab default configuration if necessary
     * @param gitlabInfo Received gitlab info
     * @return Initialized API for Gitlab
     */
    open fun overrideGitlab(gitlabInfo: GitlabInfo): GitLabApi {
        val gitlab = gitlabAdmin

        // If gitlabInfo.token is empty assure using values found in application.yml.
        // i.e. those in default GitlabAdmin object
        if (StringUtils.isEmpty(gitlabInfo.token)) {
            logger.info("Already using default url and token")
        } else {
            // If gitlabInfo.token has a value we overide as appropriate
            if (!gitlab.api().gitLabServerUrl.equals(gitlabInfo.url, ignoreCase = true) ||
                !gitlab.api().authToken.equals(gitlabInfo.token, ignoreCase = true)) {
                logger.info("Overiding gitlab url and token")
                val api = GitLabApi(gitlabInfo.url, gitlabInfo.token)
                api.ignoreCertificateErrors = true
                return api
            }
        }
        return gitlab.api()
    }

    /**
     * Remove an existing group
     *
     * @param migration Migration containing group information
     * @throws GitLabApiException Problem when removing
     */
    @Throws(GitLabApiException::class)
    open fun removeGroup(migration: Migration) {

        //Get default GitlabAdmin object
        try {
            val elements = migration.svnProject.split("/").toTypedArray()
            val namespace = StringBuffer(migration.gitlabGroup)
            IntStream.range(1, elements.size)
                .forEach { i: Int -> namespace.append(String.format("/%s", elements[i])) }
            val projects = gitlabAdmin.projectApi().getProjects(elements[elements.size - 1])
            val project = projects.stream().filter { p: Project -> p.pathWithNamespace.equals(namespace.toString(), ignoreCase = true) }.findFirst().get()
            gitlabAdmin.projectApi().deleteProject(project)
            // Waiting for gitlab to delete it completely
            val maxAge = LocalDateTime.now().plus(applicationProperties.gitlab.waitSeconds.toLong(), ChronoUnit.SECONDS)
            while (gitlabAdmin.projectApi().getProject(project.id) != null || maxAge.isAfter(LocalDateTime.now())) {
            }
        } catch (apiEx: GitLabApiException) {
            if (apiEx.reason.equals("Not Found", ignoreCase = true)) {
                // Project already deleted
                logger.info("Unknown project, cannot delete it (or maybe already deleted)")
                return
            }
            val message = apiEx.message!!.replace(applicationProperties.gitlab.token, STARS)
            logger.error("Impossible to remove group", message)
            throw apiEx
        }
    }
}
