package fr.yodamad.svn2git.e2e;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.data.Repository;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MigrationManager;
import org.gitlab4j.api.GitLabApi;
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

import static fr.yodamad.svn2git.data.Repository.Files.*;
import static fr.yodamad.svn2git.data.Repository.simple;
import static fr.yodamad.svn2git.utils.Checks.isPresent;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class SimpleRepoTests {

    @Autowired
    private MigrationManager migrationManager;
    @Autowired
    private MigrationRepository migrationRepository;

    private static final GitLabApi gitLabApi = new GitLabApi("https://tanuki.yodamad.fr", "6UQZDV_j-gm4vz-NGxbJ");

    @Before
    public void cleanGitlab() throws GitLabApiException {
        Optional<Project> project = gitLabApi.getProjectApi().getOptionalProject("simple", "simple");
        if (project.isPresent()) gitLabApi.getProjectApi().deleteProject(project.get().getId());
    }

    private static Migration initMigration() {
        Repository repository = simple();
        Migration migration = new Migration();
        migration.setGitlabToken("6UQZDV_j-gm4vz-NGxbJ");
        migration.setGitlabUrl("https://tanuki.yodamad.fr");
        migration.setGitlabGroup(repository.namespace);
        migration.setGitlabProject("");
        migration.setUser("gitlab");

        migration.setSvnUrl("https://chaos.yodamad.fr/svn");
        migration.setSvnProject("");
        migration.setSvnUser("demo");
        migration.setSvnPassword("demo");
        migration.setSvnGroup(repository.name);

        return migration;
    }

    @Test
    public void test_full_migration_on_simple_repo() throws ExecutionException, InterruptedException, GitLabApiException {
        Migration migration = initMigration();
        migration.setSvnHistory("all");
        migration.setTrunk("trunk");
        migration.setBranches("*");
        migration.setTags("*");

        Migration saved = migrationRepository.save(migration);
        Future<String> result = migrationManager.startMigration(saved.getId(), false);
        // Wait for async
        result.get();

        Migration closed = migrationRepository.findById(saved.getId()).get();
        assertThat(closed.getStatus()).isEqualTo(StatusEnum.DONE_WITH_WARNINGS);

        // Check project
        Optional<Project> project = gitLabApi.getProjectApi().getOptionalProject(simple().namespace, simple().name);
        assertThat(project.isPresent()).isTrue();

        // Check files
        isPresent(project.get(), REVISION, true);
        isPresent(project.get(), FILE_BIN, false);
        isPresent(project.get(), ANOTHER_BIN, false);
        isPresent(project.get(), JAVA, false);
        isPresent(project.get(), DEEP, false);

        // Check branches
        List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(project.get().getId());
        assertThat(branches).hasSize(3);

        // Check tags
        List<Tag> tags = gitLabApi.getTagsApi().getTags(project.get().getId());
        assertThat(tags).hasSize(2);
    }
}

