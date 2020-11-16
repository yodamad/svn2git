package fr.yodamad.svn2git.service.client

import fr.yodamad.svn2git.config.ApplicationProperties
import org.apache.commons.lang3.StringUtils
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.File

/**
 * Artifactory administration tool to interact with the tool
 */
@Component
open class ArtifactoryAdmin(applicationProperties: ApplicationProperties) {
    private var artifactory: Artifactory? = null
    private val defaultRepository: String
    private val groupIdPrefix: String
    private val uploadPauseMilliSeconds: Long

    /**
     * Upload a binary to artifactory
     * @param artifact Binary to upload
     * @param groupId groupId to add to default groupId
     * @param artifactId Artifact name
     * @param version Artifact version
     *
     * @return artifactPath used to upload the artifact
     */
    open fun uploadArtifact(artifact: File, groupId: String?, artifactId: String?, version: String?): String {
        LOG.info(String.format("Upload file %s to artifactory", artifact.name))
        val artifactPath = String.format("%s/%s%s/%s/%s",
            groupIdPrefix.replace(".", "/"),
            groupId,
            artifactId,
            version,
            artifact.name)
        artifactory!!.repository(defaultRepository).upload(artifactPath, artifact).doUpload()

        // To avoid overloading Artifactory
        if (uploadPauseMilliSeconds > 0) {
            try {
                LOG.info(String.format("Waiting uploadPauseMilliSeconds:%s", uploadPauseMilliSeconds))
                Thread.sleep(uploadPauseMilliSeconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw RuntimeException(e)
            }
        }
        return artifactPath
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ArtifactoryAdmin::class.java)
    }

    init {
        artifactory = if (!StringUtils.isEmpty(applicationProperties.artifactory.password)) {
            ArtifactoryClientBuilder.create()
                .setUrl(applicationProperties.artifactory.url)
                .setUsername(applicationProperties.artifactory.user)
                .setPassword(applicationProperties.artifactory.password)
                .build()
        } else {
            ArtifactoryClientBuilder.create()
                .setUrl(applicationProperties.artifactory.url)
                .setUsername(applicationProperties.artifactory.user)
                .setAccessToken(applicationProperties.artifactory.accessToken)
                .build()
        }
        defaultRepository = applicationProperties.artifactory.repository
        groupIdPrefix = applicationProperties.artifactory.groupIdPrefix
        uploadPauseMilliSeconds = applicationProperties.artifactory.uploadPauseMilliSeconds
    }
}
