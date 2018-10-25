package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Controller to use Gitlab API
 */
@RestController
@RequestMapping("/api/gitlab/")
public class GitlabResource {

    /** Gitlab API wrapper. */
    private final GitlabAdmin gitlabAdmin;

    public GitlabResource(GitlabAdmin gitlabAdmin) {
        this.gitlabAdmin = gitlabAdmin;
    }

    /**
     * Check if a user exists on Gitlab
     * @param userName User ID search
     * @return if user found
     */
    @GetMapping("user/{username}")
    @Timed
    public ResponseEntity<Boolean> checkUser(@PathVariable("username") String userName) {
        Optional<User> user = gitlabAdmin.userApi().getOptionalUser(userName);

        return ResponseEntity.ok()
                .body(user.isPresent());
    }

    /**
     * Check if a group exists on Gitlab
     * @param groupName Group name search
     * @return if group found
     */
    @GetMapping("group/{groupName}")
    @Timed
    public ResponseEntity<Boolean> checkGroup(@PathVariable("groupName") String groupName) {
        Optional<Group> group = gitlabAdmin.groupApi().getOptionalGroup(groupName);

        return ResponseEntity.ok()
            .body(group.isPresent());
    }
}
