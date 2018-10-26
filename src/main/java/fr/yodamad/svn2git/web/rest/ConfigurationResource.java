package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose API to retrieve some configuration elements
 */
@RestController
@RequestMapping("/api/config/")
public class ConfigurationResource {

    /** SVN Url. */
    @Value("${svn.url}") private String svnUrl;
    /** Gitlab Url. */
    @Value("${gitlab.url}") private String gitlabUrl;

    /**
     * @return Configured SVN URL
     */
    @Timed
    @GetMapping("svn")
    public ResponseEntity<String> getSvnUrl() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(svnUrl);
    }

    /**
     * @return Configured Gitlab URL
     */
    @Timed
    @GetMapping("gitlab")
    public ResponseEntity<String> getGitlabUrl() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(gitlabUrl);
    }

}
