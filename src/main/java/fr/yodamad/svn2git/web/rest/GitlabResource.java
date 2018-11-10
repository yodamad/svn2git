package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.GitlabInfo;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.User;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${gitlab.url}") String gitlabUrl;

    public GitlabResource(GitlabAdmin gitlabAdmin) {
        this.gitlabAdmin = gitlabAdmin;
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
        if (!gitlabUrl.equalsIgnoreCase(gitlabInfo.url)) {
            gitlab = new GitlabAdmin(gitlabInfo.url, gitlabInfo.token);
        }
        Optional<User> user = gitlab.userApi().getOptionalUser(userName);

        return ResponseEntity.ok()
                .body(user.isPresent());
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
        if (!gitlabUrl.equalsIgnoreCase(gitlabInfo.url)) {
            gitlab = new GitlabAdmin(gitlabInfo.url, gitlabInfo.token);
        }
        Optional<Group> group = gitlab.groupApi().getOptionalGroup(groupName);

        return ResponseEntity.ok()
            .body(group.isPresent());
    }
}
