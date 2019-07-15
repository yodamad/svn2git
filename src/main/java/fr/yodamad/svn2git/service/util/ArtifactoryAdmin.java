package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.config.ApplicationProperties;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Artifactory administration tool to interact with the tool
 */
@Component
public class ArtifactoryAdmin {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactoryAdmin.class);
    private Artifactory artifactory;
    private String defaultRepository;
    private String groupIdPrefix;

    public ArtifactoryAdmin(ApplicationProperties applicationProperties) {
        artifactory = ArtifactoryClientBuilder.create()
            .setUrl(applicationProperties.artifactory.url)
            .setUsername(applicationProperties.artifactory.user)
            .setPassword(applicationProperties.artifactory.password)
            .build();
        defaultRepository = applicationProperties.artifactory.repository;
        groupIdPrefix = applicationProperties.artifactory.groupIdPrefix;
    }

    /**
     * Upload a binary to artifactory
     * @param artifact Binary to upload
     * @param groupId groupId to add to default groupId
     * @param artifactId Artifact name
     * @param version Artifact version
     *
     * @return artifactPath used to upload the artifact
     */
    public String uploadArtifact(File artifact, String groupId, String artifactId, String version) {
        LOG.info(String.format("Upload file %s to artifactory", artifact.getName()));
        String artifactPath =
            String.format("%s/%s%s/%s/%s",
                groupIdPrefix.replace(".", "/"),
                groupId,
                artifactId,
                version,
                artifact.getName());

        artifactory.repository(defaultRepository).upload(artifactPath, artifact).doUpload();

        return artifactPath;
    }
}
