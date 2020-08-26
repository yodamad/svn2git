package fr.yodamad.svn2git.utils;

import fr.yodamad.svn2git.data.Repository;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.RepositoryFile;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class Checks {

    private static final GitLabApi gitLabApi = new GitLabApi("https://tanuki.yodamad.fr", "6UQZDV_j-gm4vz-NGxbJ");

    public static void isPresent(Project project, String filename, boolean checkSize) {
        gitLabApi.enableRequestResponseLogging();
        Optional<RepositoryFile> file = gitLabApi.getRepositoryFileApi().getOptionalFileInfo(project.getId(), filename, "master");
        assertThat(file.isPresent()).isTrue();
        if (checkSize) {
            assertThat(file.get().getSize()).isGreaterThan(0);
        }
    }

    @Test
    public void test() {
        gitLabApi.enableRequestResponseLogging();
        Optional<RepositoryFile> file = gitLabApi.getRepositoryFileApi().getOptionalFileInfo(25, Repository.Files.DEEP, "master");
        assertThat(file.isPresent()).isTrue();
    }
}
