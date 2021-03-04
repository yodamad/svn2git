package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import fr.yodamad.svn2git.config.ApplicationProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Expose API to retrieve some configuration elements
 */
@RestController
@RequestMapping("$API$CONFIG")
open class ConfigurationResource(val applicationProperties: ApplicationProperties) {
    /**
     * @return Configured SVN URL
     */
    @Timed
    @GetMapping(SVN)
    open fun getSvnUrl(): ResponseEntity<String?>? = ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.svn.url)

    /**
     * @return Configured SVN credentials option
     */
    @Timed
    @GetMapping("$SVN/credentials")
    open fun getSvnCredentialsOption(): ResponseEntity<String?>? =
        ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.svn.credentials)

    /**
     * @return Configured svnUrlModifiable option
     */
    @Timed
    @GetMapping("$SVN/svnUrlModifiable")
    open fun getSvnUrlModifiableOption(): ResponseEntity<String?>? = ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.svn.svnUrlModifiable)

    /**
     * @return Configured svn depth searched authorized
     */
    @Timed
    @GetMapping("$SVN/depth")
    open fun getSvnDepthSearch(): ResponseEntity<String>? = ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.work.maxSvnLevel.toString())

    /**
     * @return Configured Gitlab URL
     */
    @Timed
    @GetMapping(GITLAB)
    open fun getGitlabUrl(): ResponseEntity<String?>? = ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.gitlab.url)

    /**
     * @return Configured gitlab credentials option
     */
    @Timed
    @GetMapping("$GITLAB/credentials")
    open fun getGitlabCredentialsOption(): ResponseEntity<String?>? = ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(applicationProperties.gitlab.credentials)

    /**
     * @return Configured extensions policy
     */
    @Timed
    @GetMapping("$OVERRIDE/extensions")
    open fun getOverrideExtensions(): ResponseEntity<Boolean?>? = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(applicationProperties.override.extensions)

    /**
     * @return Configured mappings policy
     */
    @Timed
    @GetMapping("$OVERRIDE/mappings")
    open fun getOverrideMappings(): ResponseEntity<Boolean?>? = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(applicationProperties.override.mappings)

    @Timed
    @GetMapping("$FLAGS/projectCleaningOption")
    open fun getProjectCleaningOption(): ResponseEntity<Boolean?>? = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(applicationProperties.flags.projectCleaningOption)

    @Timed
    @GetMapping("$FLAGS/gitlabGroupCreationOption")
    open fun getGitlabGroupCreationOption(): ResponseEntity<Boolean?>? = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(applicationProperties.flags.gitlabGroupCreationOption)

    /**
     * @return Configured Artifactory URL
     */
    @Timed
    @GetMapping(ARTIFACTORY)
    open fun getArtifactoryUrl(): ResponseEntity<String?>? = ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body(applicationProperties.artifactory.url)

    /**
     * @return Configured Nexus URL
     */
    @Timed
    @GetMapping(NEXUS)
    open fun getNexusUrl(): ResponseEntity<String?>? = ResponseEntity.ok()
        .contentType(MediaType.TEXT_PLAIN)
        .body(applicationProperties.nexus.url)
}
