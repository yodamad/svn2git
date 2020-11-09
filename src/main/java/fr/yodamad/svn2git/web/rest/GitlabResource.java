package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.GitlabInfo;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static fr.yodamad.svn2git.service.util.MigrationConstants.STARS;

/**
 * Controller to use Gitlab API
 */
@RestController
@RequestMapping("/api/gitlab/")
public class GitlabResource {

    private static final Logger LOG = LoggerFactory.getLogger(GitlabResource.class);

    private final GitlabAdmin gitlabAdmin;
    private final ApplicationProperties applicationProperties;

    public GitlabResource(GitlabAdmin gitlabAdmin,
                          ApplicationProperties applicationProperties) {
        this.gitlabAdmin = gitlabAdmin;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Check if a user exists on Gitlab
     *
     * @param userName User ID search
     * @return if user found
     */
    @PostMapping("user/{username}")
    @Timed
    public ResponseEntity<Boolean> checkUser(@PathVariable("username") String userName, @RequestBody GitlabInfo gitlabInfo) {
        UserApi api = overrideGitlab(gitlabInfo).getUserApi();
        Optional<User> user = api.getOptionalUser(userName);
        GitLabApiException exception = GitLabApi.getOptionalException(user);

        if (exception != null) {
            LOG.error("Fail to access gitlab", exception);
            return ResponseEntity.badRequest().build();
        }

        if (user.isPresent()) {
            return ResponseEntity.ok().body(user.isPresent());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if a group exists on Gitlab, AND that the passed in userName is a member of that group
     *
     * @param groupName Group name search
     * @param userName  Member of group we are checking for
     * @return if group found
     */
    @PostMapping("group/{groupName}/members/{userName}")
    @Timed
    public ResponseEntity<Boolean> checkGroup(@PathVariable("groupName") String groupName,
                                              @PathVariable("userName") String userName,
                                              @RequestBody GitlabInfo gitlabInfo) {

        GitLabApi gitlab = overrideGitlab(gitlabInfo);
        // Group where project will be created
        Optional<Group> group = gitlab.getGroupApi().getOptionalGroup(groupName);

        // User that will be used to create a project
        Optional<User> user = null;
        // if token is blank / emtpy, we use the default gitlab user
        if (StringUtils.isBlank(gitlabInfo.token)) {
            user = gitlab.getUserApi().getOptionalUser(applicationProperties.gitlab.account);
        } else {
            // Username is the username that is checked in first step of wizard
            user = gitlab.getUserApi().getOptionalUser(userName);
        }

        // if the group is present and the user that will use it is present
        if (group.isPresent() && user.isPresent()) {

            try {

                // check if userName passed in is a member of the group (includingInherited)
                // An exception is thrown if member is not found in group.
                Member member = gitlab.getGroupApi().getMember(group.get().getId(), user.get().getId());
                if (member != null) {
                    LOG.info(String.format("User:%s is a member of the target group:%s", user.get().getUsername(), group.get().getName()));

                    // Group exists and the passed in username is a member of it
                    return ResponseEntity.ok()
                        .body(group.isPresent());

                } else {
                    // This should not be possible
                    LOG.error(String.format("Member:%s not found in group:%s",
                        user.get().getUsername(), group.get().getName()));

                    // Group exists but an unexpected error getting user
                    return ResponseEntity.notFound().build();
                }

            } catch (GitLabApiException apiEx) {

                // just in case token in message
                String message = apiEx.getMessage().replace(applicationProperties.gitlab.token, STARS);

                if (apiEx.getReason().equalsIgnoreCase("Not Found")) {
                    // UserName not found : is a possible case. i.e. we only raise a warning in logs.
                    LOG.warn(String.format("Member:%s not found in group:%s",
                        user.get().getUsername(), group.get().getName()));
                } else {
                    LOG.error("Error getting member of group", message);
                }

                // User didn't exist in group
                return ResponseEntity.notFound().build();
            }

        } else {
            // Group didn't exist or user didn't exist
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Creates a group on Gitlab
     *
     * @param groupName Group name to create
     * @return
     */
    @PutMapping("group/{groupName}")
    @Timed
    public ResponseEntity<Boolean> createGroup(@PathVariable("groupName") String groupName, @RequestBody GitlabInfo gitlabInfo) {
        GitLabApi gitlab = overrideGitlab(gitlabInfo);
        Group group = new Group();
        group.setName(groupName);
        group.setPath(groupName);
        group.setVisibility(Visibility.INTERNAL);
        try {
            gitlab.getGroupApi().addGroup(group);
            return ResponseEntity.ok().body(true);
        } catch (GitLabApiException apiEx) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Override Gitlab default configuration if necessary
     *
     * @param gitlabInfo Received gitlab info
     * @return
     */
    private GitLabApi overrideGitlab(GitlabInfo gitlabInfo) {

        GitlabAdmin gitlab = gitlabAdmin;

        // If gitlabInfo.token is empty assure using values found in application.yml.
        // i.e. those in default GitlabAdmin object
        if (StringUtils.isEmpty(gitlabInfo.token)) {
            LOG.info("Already using default url and token");
        } else {
            // If gitlabInfo.token has a value we overide as appropriate
            if (!gitlab.api().getGitLabServerUrl().equalsIgnoreCase(gitlabInfo.url) ||
                !gitlab.api().getAuthToken().equalsIgnoreCase(gitlabInfo.token)) {

                LOG.info("Overiding gitlab url and token");
                GitLabApi api = new GitLabApi(gitlabInfo.url, gitlabInfo.token);
                api.setIgnoreCertificateErrors(true);
                return api;
            }
        }
        return gitlab.api();
    }

    /**
     * Remove an existing group
     *
     * @param migration Migration containing group information
     * @throws GitLabApiException Problem when removing
     */
    public void removeGroup(Migration migration) throws GitLabApiException {

        //Get default GitlabAdmin object

        try {
            String[] elements = migration.getSvnProject().split("/");
            StringBuffer namespace = new StringBuffer(migration.getGitlabGroup());
            IntStream.range(1, elements.length)
                .forEach(i -> namespace.append(String.format("/%s", elements[i])));
            List<Project> projects = gitlabAdmin.projectApi().getProjects(elements[elements.length - 1]);
            Project project = projects.stream().filter(p -> p.getPathWithNamespace().equalsIgnoreCase(namespace.toString())).findFirst().get();
            gitlabAdmin.projectApi().deleteProject(project);
            // Waiting for gitlab to delete it completely
            LocalDateTime maxAge = LocalDateTime.now().plus(applicationProperties.gitlab.waitSeconds, ChronoUnit.SECONDS);
            while (gitlabAdmin.projectApi().getProject(project.getId()) != null || maxAge.isAfter(LocalDateTime.now())) {
            }
        } catch (GitLabApiException apiEx) {
            if (apiEx.getReason().equalsIgnoreCase("Not Found")) {
                // Project already deleted
                LOG.info("Unknown project, cannot delete it (or maybe already deleted)");
                return;
            }
            String message = apiEx.getMessage().replace(applicationProperties.gitlab.token, STARS);
            LOG.error("Impossible to remove group", message);
            throw apiEx;
        }
    }
}
