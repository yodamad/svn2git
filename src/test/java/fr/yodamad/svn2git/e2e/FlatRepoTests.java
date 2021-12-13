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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static fr.yodamad.svn2git.data.Repository.Dirs.FOLDER;
import static fr.yodamad.svn2git.data.Repository.Files.DEEP_FILE;
import static fr.yodamad.svn2git.data.Repository.Files.FLAT_FILE;
import static fr.yodamad.svn2git.data.Repository.flat;
import static fr.yodamad.svn2git.utils.Checks.*;
import static fr.yodamad.svn2git.utils.MigrationUtils.initFlatMigration;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class FlatRepoTests {

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

    @BeforeEach
    public void cleanGitlab() throws GitLabApiException {
        Optional<Project> project = api.getProjectApi().getOptionalProject(flat().namespace, flat().name);
        if (project.isPresent()) api.getProjectApi().deleteProject(project.get().getId());
    }

    @AfterEach
    public void forceCleanGitlab() throws GitLabApiException, InterruptedException {
        Optional<Project> project = api.getProjectApi().getOptionalProject(flat().namespace, flat().name);
        if (project.isPresent()) api.getProjectApi().deleteProject(project.get().getId());
        while(api.getProjectApi().getOptionalProject(flat().namespace, flat().name).isPresent()) {
            Thread.sleep(500);
        }
    }

    @Test
    public void test_migration_on_flat_repository() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initFlatMigration(applicationProperties);
        migration.setSvnProject("module1");
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setTags(null);
        migration.setBranches(null);
        migration.setFlat(true);

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        isPresent(project.get(), FLAT_FILE, false);
        isPresent(project.get(), FOLDER + DEEP_FILE, false);

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
        return Checks.checkProject(flat());
    }
}
