package fr.yodamad.svn2git.e2e;

import fr.yodamad.svn2git.Svn2GitApp;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

import static fr.yodamad.svn2git.data.Repository.flat;
import static fr.yodamad.svn2git.utils.MigrationUtils.GITLAB_API;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class FlatRepoTests {

    @Before
    public void cleanGitlab() throws GitLabApiException {
        Optional<Project> project = GITLAB_API.getProjectApi().getOptionalProject(flat().namespace, flat().name);
        if (project.isPresent()) GITLAB_API.getProjectApi().deleteProject(project.get().getId());
    }
}
