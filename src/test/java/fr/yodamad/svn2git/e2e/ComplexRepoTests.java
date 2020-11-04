package fr.yodamad.svn2git.e2e;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.utils.Checks;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Tag;
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

import static fr.yodamad.svn2git.data.Repository.complex;
import static fr.yodamad.svn2git.utils.Checks.*;
import static fr.yodamad.svn2git.utils.MigrationUtils.*;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class ComplexRepoTests {

    @Autowired
    private MigrationManager migrationManager;
    @Autowired
    private MigrationRepository migrationRepository;

    @Before
    public void cleanGitlab() throws GitLabApiException {
        String projectName = complex().name.split("/")[1];
        String subGroup = complex().name.split("/")[0];
        String group = format("%s/%s", complex().namespace, subGroup);
        Optional<Project> project = GITLAB_API.getProjectApi().getOptionalProject(group, projectName);
        if (project.isPresent()) GITLAB_API.getProjectApi().deleteProject(project.get().getId());
    }

    @Test
    public void test_migration_on_flat_repository() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initComplexMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setTags("*");
        migration.setBranches("*");

        startAndCheck(migration);

        // Check project
        Optional<Project> project = checkProject();

        // Check files
        checkAllFiles(project);

        // Check branches
        List<Branch> branches = checkBranches(project);
        branches.stream().filter(b -> !b.getName().equals("master")).forEach(b -> hasNoHistory(project, b.getName()));
        branches.stream().filter(b -> b.getName().equals("master")).forEach(b -> hasHistory(project, b.getName()));

        // Check tags
        List<Tag> tags = checkTags(project);
        tags.forEach(t -> hasNoHistory(project, t.getName()));
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
        return Checks.checkProject(complex());
    }
}
