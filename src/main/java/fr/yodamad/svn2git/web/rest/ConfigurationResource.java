package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.config.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * @return Configured SVN URL
     */
    @Timed
    @GetMapping("svn")
    public ResponseEntity<String> getSvnUrl() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.svn.url);
    }

    /**
     * @return Configured SVN credentials option
     */
    @Timed
    @GetMapping("svn/credentials")
    public ResponseEntity<String> getSvnCredentialsOption() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.svn.credentials);
    }

    /**
     * @return Configured Gitlab URL
     */
    @Timed
    @GetMapping("gitlab")
    public ResponseEntity<String> getGitlabUrl() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.gitlab.url);
    }

    /**
     * @return Configured gitlab credentials option
     */
    @Timed
    @GetMapping("gitlab/credentials")
    public ResponseEntity<String> getGitlabCredentialsOption() {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.gitlab.credentials);
    }

    /**
     * @return Configured extensions policy
     */
    @Timed
    @GetMapping("override/extensions")
    public ResponseEntity<Boolean> getOverrideExtensions() {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(applicationProperties.override.extensions);
    }
    /**
     * @return Configured mappings policy
     */
    @Timed
    @GetMapping("override/mappings")
    public ResponseEntity<Boolean> getOverrideMappings() {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(applicationProperties.override.mappings);
    }
}
