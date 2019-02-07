package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.GitlabInfo;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller to use Gitlab API
 */
@RestController
@RequestMapping("/api/gitlab/")
public class GitlabResource {

    /** Gitlab API wrapper. */
    private final GitlabAdmin gitlabAdmin;
    private final ApplicationProperties applicationProperties;

    public GitlabResource(GitlabAdmin gitlabAdmin,
                          ApplicationProperties applicationProperties) {
        this.gitlabAdmin = gitlabAdmin;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Check if a user exists on Gitlab
     * @param userName User ID search
     * @return if user found
     */
    @PostMapping("user/{username}")
    @Timed
    public ResponseEntity<Boolean> checkUser(@PathVariable("username") String userName, @RequestBody GitlabInfo gitlabInfo) {
        GitlabAdmin gitlab = gitlabAdmin;
        if (!applicationProperties.gitlab.url.equalsIgnoreCase(gitlabInfo.url)) {
            gitlabAdmin.setGitLabApi(customApi(gitlabInfo));
        }
        Optional<User> user = gitlab.userApi().getOptionalUser(userName);

        if (user.isPresent()) {
            return ResponseEntity.ok()
                .body(user.isPresent());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if a group exists on Gitlab
     * @param groupName Group name search
     * @return if group found
     */
    @PostMapping("group/{groupName}")
    @Timed
    public ResponseEntity<Boolean> checkGroup(@PathVariable("groupName") String groupName, @RequestBody GitlabInfo gitlabInfo) {
        GitlabAdmin gitlab = gitlabAdmin;
        if (!applicationProperties.gitlab.url.equalsIgnoreCase(gitlabInfo.url)) {
            gitlabAdmin.setGitLabApi(customApi(gitlabInfo));
        }
        Optional<Group> group = gitlab.groupApi().getOptionalGroup(groupName);

        if (group.isPresent()) {
            return ResponseEntity.ok()
                .body(group.isPresent());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private GitLabApi customApi(GitlabInfo gitlabInfo) {
        GitLabApi api = new GitLabApi(gitlabInfo.url, gitlabInfo.token);
        return api;
    }
}
