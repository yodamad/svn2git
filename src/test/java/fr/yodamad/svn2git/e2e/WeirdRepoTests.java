package fr.yodamad.svn2git.e2e;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.utils.Checks;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static fr.yodamad.svn2git.data.Repository.Files.*;
import static fr.yodamad.svn2git.data.Repository.weird;
import static fr.yodamad.svn2git.utils.Checks.*;
import static fr.yodamad.svn2git.utils.MigrationUtils.initWeirdMigration;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class WeirdRepoTests {

    @Autowired
    private MigrationManager migrationManager;
    @Autowired
    private MigrationRepository migrationRepository;
    @Autowired
    private ApplicationProperties applicationProperties;
    private GitLabApi api;

    @PostConstruct
    public void initApi() {
        api = new GitLabApi(applicationProperties.gitlab.url, applicationProperties.gitlab.token);
        Checks.initApi(applicationProperties);
    }

    @Before
    public void cleanGitlab() throws GitLabApiException {
        Optional<Project> project = api.getProjectApi().getOptionalProject(weird().namespace, weird().name);
        if (project.isPresent()) api.getProjectApi().deleteProject(project.get().getId());
    }

    @After
    public void forceCleanGitlab() throws GitLabApiException, InterruptedException {
        Optional<Project> project = api.getProjectApi().getOptionalProject(weird().namespace, weird().name);
        if (project.isPresent()) api.getProjectApi().deleteProject(project.get().getId());
        while(api.getProjectApi().getOptionalProject(weird().namespace, weird().name).isPresent()) {
            sleep(500);
        }
    }

    @Test
    public void test_migration_with_space_in_trunk_name() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initWeirdMigration(applicationProperties);
        migration.setSvnHistory("all");
        migration.setTrunk("branch with space");
        migration.setTags(null);
        migration.setBranches("*");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        isPresent(project.get(), ROOT_ANOTHER_BIN, false);
        isPresent(project.get(), FILE_BIN, false);
        isPresent(project.get(), REVISION, false);

        // Check branches
        List<Branch> branches = checkBranches(project, 3);

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
        return Checks.checkProject(weird());
    }
}
