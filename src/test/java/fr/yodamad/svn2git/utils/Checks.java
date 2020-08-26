package fr.yodamad.svn2git.utils;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;

import java.util.List;
import java.util.Optional;

import static fr.yodamad.svn2git.data.Repository.Files.*;
import static fr.yodamad.svn2git.data.Repository.simple;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class Checks {

    private static final GitLabApi gitLabApi = new GitLabApi("https://tanuki.yodamad.fr", "6UQZDV_j-gm4vz-NGxbJ");

    public static Optional<Project> checkProject() {
        Optional<Project> project = gitLabApi.getProjectApi().getOptionalProject(simple().namespace, simple().name);
        assertThat(project).isPresent();
        return project;
    }

    public static List<Branch> checkBranches(Optional<Project> project) throws GitLabApiException {
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(project.get().getId());
        assertThat(branches).hasSize(3);
        return branches;
    }

    public static List<Tag> checkTags(Optional<Project> project) throws GitLabApiException {
        List<Tag> tags = gitLabApi.getTagsApi().getTags(project.get().getId());
        assertThat(tags).hasSize(2);
        return tags;
    }

    public static void hasHistory(Optional<Project> project, String ref) {
        try {
            List<Commit> branchCommits = gitLabApi.getCommitsApi().getCommits(project.get().getId(), ref, null);
            assertThat(branchCommits.size()).isGreaterThan(1);
        } catch (GitLabApiException e) {
            fail();
        }
    }

    public static void hasNoHistory(Optional<Project> project, String ref) {
        try {
            List<Commit> branchCommits = gitLabApi.getCommitsApi().getCommits(project.get().getId(), ref, null);
            assertThat(branchCommits.size()).isEqualTo(1);
        } catch (GitLabApiException e) {
            fail();
        }
    }

    public static void isPresent(Project project, String filename, boolean checkSize) {
        gitLabApi.enableRequestResponseLogging();
        Optional<RepositoryFile> file = gitLabApi.getRepositoryFileApi().getOptionalFileInfo(project.getId(), filename, "master");
        assertThat(file.isPresent()).isTrue();
        if (checkSize) {
            assertThat(file.get().getSize()).isGreaterThan(0);
        }
    }

    public static void checkAllFiles(Optional<Project> project) {
        isPresent(project.get(), REVISION, true);
        isPresent(project.get(), FILE_BIN, false);
        isPresent(project.get(), ANOTHER_BIN, false);
        isPresent(project.get(), JAVA, false);
        isPresent(project.get(), DEEP, false);
    }
}
