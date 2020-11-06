package fr.yodamad.svn2git.e2e;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.utils.Checks;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Tag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static fr.yodamad.svn2git.data.Repository.Branches.*;
import static fr.yodamad.svn2git.data.Repository.Tags.V1_1;
import static fr.yodamad.svn2git.data.Repository.simple;
import static fr.yodamad.svn2git.utils.Checks.*;
import static fr.yodamad.svn2git.utils.MigrationUtils.GITLAB_API;
import static fr.yodamad.svn2git.utils.MigrationUtils.initSimpleMigration;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class SimpleRepoTests {

    @Autowired
    private MigrationManager migrationManager;
    @Autowired
    private MigrationRepository migrationRepository;

    @Before
    public void checkGitlab() throws GitLabApiException {
        Optional<Project> project = GITLAB_API.getProjectApi().getOptionalProject(simple().namespace, simple().name);
        if (project.isPresent()) {
            GITLAB_API.getProjectApi().deleteProject(project.get().getId());
        }
    }

    @After
    public void cleanGitlab() throws GitLabApiException {
        Optional<Project> project = GITLAB_API.getProjectApi().getOptionalProject(simple().namespace, simple().name);
        if (project.isPresent()) GITLAB_API.getProjectApi().deleteProject(project.get().getId());
        GITLAB_API.getProjectApi().getOptionalProject(simple().namespace, simple().name);
    }

    @Test
    public void test_full_migration_on_simple_repo() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setTags("*");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasHistory(project, t.getName()));
    }

    @Test
    public void test_full_migration_on_simple_repo_without_history() throws GitLabApiException, ExecutionException, InterruptedException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("nothing");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setTags("*");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Commit> commits = GITLAB_API.getCommitsApi().getCommits(project.get().getId(), MASTER, null);
        assertThat(commits.size()).isEqualTo(2);
        List<Branch> branches = checkBranches(project);
        branches.stream()
            .filter(b -> !MASTER.equals(b.getName()))
            .forEach(b -> hasNoHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasNoHistory(project, t.getName()));
    }

    @Test
    public void test_full_migration_on_simple_repo_with_history_on_trunk() throws GitLabApiException, ExecutionException, InterruptedException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("trunk");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setTags("*");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        hasHistory(project, MASTER);
        List<Branch> branches = checkBranches(project);
        branches.stream()
            .filter(b -> !MASTER.equals(b.getName()))
            .forEach(b -> hasNoHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasNoHistory(project, t.getName()));
    }

    @Test
    public void test_trunk_only_migration_on_simple_repo() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches(null);
        migration.setTags(null);

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project, 1);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        checkTags(project, 0);
    }

    @Test
    public void test_migration_on_simple_repo_without_branches() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches(null);
        migration.setTags("*");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project, 1);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasHistory(project, t.getName()));
    }

    @Test
    public void test_migration_on_simple_repo_without_tags() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setTags(null);

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        checkTags(project, 0);
    }

    @Test
    public void test_migration_on_simple_repo_with_filtered_branches_and_tags() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setBranchesToMigrate(FEATURE);
        migration.setTags("*");
        migration.setTagsToMigrate(V1_1);

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project, 2);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project, 1);
        tags.forEach(t -> hasHistory(project, t.getName()));
    }

    @Test
    public void test_full_migration_on_simple_repo_filtering_extensions() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setTags("*");
        migration.setForbiddenFileExtensions("*.bin");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkOnlyNotBinFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasHistory(project, t.getName()));
    }

    @Test
    public void test_full_migration_with_dev_as_master() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("dev");
        migration.setBranches("*");
        migration.setTags("*");
        migration.setForbiddenFileExtensions("*.bin");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkOnlyNotBinFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project, 2);
        branches.forEach(b -> hasHistory(project, b.getName()));
        boolean notFound = branches.stream().noneMatch(b -> DEV.equals(b.getName()));
        assertThat(notFound).isTrue();

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasHistory(project, t.getName()));
    }

    private void startAndCheck(Migration migration) throws ExecutionException, InterruptedException {
        Migration saved = migrationRepository.save(migration);
        Future<String> result = migrationManager.startMigration(saved.getId(), false);
        // Wait for async
        result.get();

        Migration closed = migrationRepository.findById(saved.getId()).get();
        assertThat(closed.getStatus()).isEqualTo(StatusEnum.DONE_WITH_WARNINGS);
    }

    private static Optional<Project> checkProject() {
        return Checks.checkProject(simple());
    }
}

