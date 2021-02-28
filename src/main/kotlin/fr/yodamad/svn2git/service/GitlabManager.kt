package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.service.client.GitlabAdmin
import fr.yodamad.svn2git.service.util.STARS
import org.apache.commons.lang3.StringUtils.isEmpty
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import org.gitlab4j.api.models.Visibility
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Service

@Service
open class GitlabManager(val historyMgr: HistoryManager,
                    var applicationProperties: ApplicationProperties) {

    private val LOG = LoggerFactory.getLogger(GitlabManager::class.java)

    /**
     * Get freshinstance of GitlabAdmin object when called.
     *
     * @return GitlabAdmin
     */
    @Lookup
    open fun getGitlabAdminPrototype(): GitlabAdmin? { return null }

    /**
     * Create project in GitLab
     *
     * @param migration
     * @param workUnit
     * @throws GitLabApiException
     */
    @Throws(GitLabApiException::class)
    open fun createGitlabProject(migration: Migration) : Int {
        val history: MigrationHistory = historyMgr.startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, migration.gitlabUrl + migration.gitlabGroup)
        var gitlabAdmin = getGitlabAdminPrototype()
        var gitlabProjectId : Int

        // If gitlabInfo.token is empty assure using values found in application.yml.
        // i.e. those in default GitlabAdmin object
        if (isEmpty(migration.gitlabToken)) {
            LOG.info("Already using default url and token")
        } else {
            // If gitlabInfo.token has a value we overide as appropriate
            if (!gitlabAdmin!!.api().gitLabServerUrl.equals(migration.gitlabUrl, ignoreCase = true) ||
                !gitlabAdmin.api().authToken.equals(migration.gitlabToken, ignoreCase = true)) {
                LOG.info("Overiding gitlab url and token")
                gitlabAdmin = GitlabAdmin(applicationProperties)
                val api = GitLabApi(migration.gitlabUrl, migration.gitlabToken)
                api.ignoreCertificateErrors = true
                gitlabAdmin.setGitlabApi(api)
            }
        }
        try {
            val group = gitlabAdmin!!.groupApi().getGroup(migration.gitlabGroup)

            // If no svn project specified, use svn group instead
            if (isEmpty(migration.svnProject) && isEmpty(migration.gitlabProject)) {
                gitlabProjectId = gitlabAdmin.projectApi().createProject(group.id, migration.svnGroup).id
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } else {
                // split svn structure to create gitlab elements (group(s), project)
                val projectFullName = if (isEmpty(migration.gitlabProject)) migration.svnProject else migration.gitlabProject
                val structure = projectFullName.split("/").toTypedArray()
                var groupId = group.id
                var currentPath = group.path
                if (structure.size > 2) {
                    for (module in 1 until structure.size - 1) {
                        val gitlabSubGroup = Group()
                        gitlabSubGroup.name = structure[module]
                        gitlabSubGroup.path = structure[module]
                        gitlabSubGroup.visibility = Visibility.INTERNAL
                        currentPath += String.format("/%s", structure[module])
                        gitlabSubGroup.parentId = groupId
                        try {
                            groupId = gitlabAdmin.groupApi().addGroup(gitlabSubGroup).id
                        } catch (gitlabApiEx: GitLabApiException) {
                            // Ignore error & get existing groupId
                            groupId = gitlabAdmin.groupApi().getGroup(currentPath).id
                            continue
                        }
                    }
                }
                val project = gitlabAdmin.groupApi().getProjects(groupId)
                    .stream()
                    .filter { p: Project -> p.name.equals(structure[structure.size - 1], ignoreCase = true) }
                    .findFirst()
                if (!project.isPresent) {
                    gitlabProjectId = gitlabAdmin.projectApi().createProject(groupId, structure[structure.size - 1]).id
                    historyMgr.endStep(history, StatusEnum.DONE, null)
                } else {
                    throw GitLabApiException("Please remove the destination project '${group.name}/${structure[structure.size - 1]}'")
                }
            }
            return gitlabProjectId
        } catch (exc: GitLabApiException) {
            val message: String? = exc.message?.replace(applicationProperties.gitlab.token, STARS)
            LOG.error("Gitlab errors are ${exc.validationErrors}")
            historyMgr.endStep(history, StatusEnum.FAILED, message)
            throw exc
        }
    }
}
