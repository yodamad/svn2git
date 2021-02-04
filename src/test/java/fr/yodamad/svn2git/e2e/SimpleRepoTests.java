package fr.yodamad.svn2git.e2e;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Mapping;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MappingRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.utils.Checks;
import org.gitlab4j.api.GitLabApi;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static fr.yodamad.svn2git.data.Repository.Branches.*;
import static fr.yodamad.svn2git.data.Repository.Dirs.DIRECTORY;
import static fr.yodamad.svn2git.data.Repository.Dirs.FOLDER;
import static fr.yodamad.svn2git.data.Repository.Tags.V1_1;
import static fr.yodamad.svn2git.data.Repository.simple;
import static fr.yodamad.svn2git.utils.Checks.*;
import static fr.yodamad.svn2git.utils.MigrationUtils.initSimpleMigration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class SimpleRepoTests {

    @Autowired
    private MigrationManager migrationManager;
    @Autowired
    private MigrationRepository migrationRepository;
    @MockBean
    private MappingRepository mappingRepository;
    @Autowired
    private ApplicationProperties applicationProperties;
    private GitLabApi api;

    @PostConstruct
    public void initApi() {
        api = new GitLabApi(applicationProperties.gitlab.url, applicationProperties.gitlab.token);
        Checks.initApi(applicationProperties);
    }

    @Before
    public void checkGitlab() throws GitLabApiException, InterruptedException {
        Optional<Project> project = api.getProjectApi().getOptionalProject(simple().namespace, simple().name);
        if (project.isPresent()) {
            api.getProjectApi().deleteProject(project.get().getId());
        }
        while(api.getProjectApi().getOptionalProject(simple().namespace, simple().name).isPresent()) {
            Thread.sleep(500);
        }
    }

    @After
    public void cleanGitlab() throws GitLabApiException, InterruptedException {
        Optional<Project> project = api.getProjectApi().getOptionalProject(simple().namespace, simple().name);
        if (project.isPresent()) api.getProjectApi().deleteProject(project.get().getId());
        while(api.getProjectApi().getOptionalProject(simple().namespace, simple().name).isPresent()) {
            Thread.sleep(500);
        }
    }

    @Test
    public void test_full_migration_on_simple_repo() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        List<Commit> commits = api.getCommitsApi().getCommits(project.get().getId(), MASTER, null);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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
        Migration migration = initSimpleMigration(applicationProperties);
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

    @Test
    public void test_migration_with_mappings() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initSimpleMigration(applicationProperties);
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches(null);
        migration.setTags(null);

        Mapping mapping = new Mapping();
        mapping.setSvnDirectory(FOLDER);
        mapping.setRegex("*");
        mapping.setGitDirectory(DIRECTORY);
        mapping.setSvnDirectoryDelete(false);
        mapping.setMigration(migration.getId());
        List<Mapping> mappings = new ArrayList<>();
        mappings.add(mapping);

        when(mappingRepository.findByMigrationAndSvnDirectoryDelete(any(), anyBoolean())).thenReturn(mappings);

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFilesWithMapping(project);

        // Check branches
        List<Branch> branches = checkBranches(project, 1);
        branches.forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        checkTags(project, 0);
    }

    private void startAndCheck(Migration migration) throws ExecutionException, InterruptedException {
        Migration saved = migrationRepository.save(migration);
        Future<String> result = migrationManager.startMigration(saved.getId(), false);
        // Wait for async
        result.get();

        Migration closed = migrationRepository.findById(saved.getId()).get();
        assertThat(closed.getStatus()).isEqualTo(StatusEnum.DONE);
    }

    private static Optional<Project> checkProject() {
        return Checks.checkProject(simple());
    }
}

