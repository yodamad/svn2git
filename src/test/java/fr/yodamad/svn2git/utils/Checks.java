package fr.yodamad.svn2git.utils;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.data.Repository;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;

import java.util.List;
import java.util.Optional;

import static fr.yodamad.svn2git.data.Repository.Dirs.FOLDER;
import static fr.yodamad.svn2git.data.Repository.Files.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class Checks {

    private static GitLabApi gitLabApi;

    public static void initApi(ApplicationProperties props) {
        gitLabApi = new GitLabApi(props.gitlab.url, props.gitlab.token);
    }

    public static Optional<Project> checkProject(Repository repository) {
        Optional<Project> project = gitLabApi.getProjectApi().getOptionalProject(repository.namespace, repository.name);
        assertThat(project).isPresent();
        return project;
    }

    public static List<Branch> checkBranches(Optional<Project> project, int nbOfBranches) throws GitLabApiException {
        gitLabApi.setIgnoreCertificateErrors(true);
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(project.get().getId());
        assertThat(branches).hasSize(nbOfBranches);
        return branches;
    }

    public static List<Branch> checkBranches(Optional<Project> project) throws GitLabApiException {
        return checkBranches(project, 3);
    }

    public static List<Tag> checkTags(Optional<Project> project, int nbOfTags) throws GitLabApiException {
        List<Tag> tags = gitLabApi.getTagsApi().getTags(project.get().getId());
        assertThat(tags).hasSize(nbOfTags);
        return tags;
    }

    public static List<Tag> checkTags(Optional<Project> project) throws GitLabApiException {
        return checkTags(project, 2);
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
        Optional<RepositoryFile> file = gitLabApi.getRepositoryFileApi().getOptionalFileInfo(project.getId(), filename, "master");
        assertThat(file.isPresent()).isTrue();
        if (checkSize) {
            assertThat(file.get().getSize()).isGreaterThan(0);
        }
    }

    public static void isFolderPresent(Project project, String filename, boolean checkSize) throws GitLabApiException {
        List<TreeItem> master = gitLabApi.getRepositoryApi().getTree(project.getId(), filename, "master", true);
        assertThat(master.size()).isGreaterThan(0);
    }

    public static void isFolderMissing(Project project, String filename, boolean checkSize) throws GitLabApiException {
        List<TreeItem> master = gitLabApi.getRepositoryApi().getTree(project.getId(), filename, "master", true);
        assertThat(master.size()).isEqualTo(0);
    }

    public static void isMissing(Project project, String filename) {
        Optional<RepositoryFile> file = gitLabApi.getRepositoryFileApi().getOptionalFileInfo(project.getId(), filename, "master");
        assertThat(file.isPresent()).isFalse();
    }

    public static void checkAllFiles(Optional<Project> project) {
        isPresent(project.get(), REVISION, true);
        isPresent(project.get(), FILE_BIN, false);
        isPresent(project.get(), ANOTHER_BIN, false);
        isPresent(project.get(), JAVA, false);
        isPresent(project.get(), DEEP, false);
    }

    public static void checkAllFilesWithMapping(Optional<Project> project) {
        isPresent(project.get(), REVISION, true);
        isPresent(project.get(), FILE_BIN, false);
        isPresent(project.get(), MAPPED_ANOTHER_BIN, false);
        isPresent(project.get(), MAPPED_JAVA, false);
        isPresent(project.get(), MAPPED_DEEP, false);
        isMissing(project.get(), FOLDER);
    }

    public static void checkOnlyNotBinFiles(Optional<Project> project) {
        isPresent(project.get(), REVISION, true);
        isMissing(project.get(), FILE_BIN);
        isMissing(project.get(), ANOTHER_BIN);
        isPresent(project.get(), JAVA, false);
        isPresent(project.get(), DEEP, false);
    }
}
