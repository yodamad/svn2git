package fr.yodamad.svn2git.utils;

import fr.yodamad.svn2git.data.Repository;
import fr.yodamad.svn2git.domain.Migration;
import org.gitlab4j.api.GitLabApi;

import static fr.yodamad.svn2git.data.Repository.flat;
import static fr.yodamad.svn2git.data.Repository.simple;

public abstract class MigrationUtils {

    private static final String GITLAB = "https://tanuki.yodamad.fr";
    private static final String TOKEN = "QkejCHCSikhJqJa357tk";

    public static final GitLabApi GITLAB_API = new GitLabApi(GITLAB, TOKEN);

    public static Migration initSimpleMigration() {
        return initMigration(simple());
    }

    public static Migration initFlatMigration() {
        Migration mig = initMigration(flat());
        mig.setFlat(true);
        return mig;
    }

    private static Migration initMigration(Repository repository) {
        Migration migration = new Migration();
        migration.setGitlabToken(TOKEN);
        migration.setGitlabUrl(GITLAB);
        migration.setGitlabGroup(repository.namespace);
        migration.setGitlabProject("");
        migration.setUser("gitlab");

        migration.setSvnUrl("https://chaos.yodamad.fr/svn");
        migration.setSvnProject("");
        migration.setSvnUser("demo");
        migration.setSvnPassword("demo");
        migration.setSvnGroup(repository.namespace);

        migration.setFlat(false);

        return migration;
    }
}
