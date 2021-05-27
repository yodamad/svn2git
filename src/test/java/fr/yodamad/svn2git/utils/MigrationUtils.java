package fr.yodamad.svn2git.utils;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.data.Repository;
import fr.yodamad.svn2git.domain.Migration;

import static fr.yodamad.svn2git.data.Repository.*;
import static java.lang.String.format;

public abstract class MigrationUtils {

    public static Migration initSimpleMigration(ApplicationProperties props) {
        return initMigration(simple(), props);
    }

    public static Migration initFlatMigration(ApplicationProperties props) {
        Migration mig = initMigration(flat(), props);
        mig.setFlat(true);
        return mig;
    }

    public static Migration initWeirdMigration(ApplicationProperties props) {
        Migration mig = initMigration(weird(), props);
        return mig;
    }

    public static Migration initComplexMigration(ApplicationProperties props) {
        Migration mig = initMigration(complex(), props);
        String name = format("/%s", complex().name);
        mig.setSvnProject(name);
        mig.setGitlabProject(name);
        return mig;
    }

    private static Migration initMigration(Repository repository, ApplicationProperties props) {
        Migration migration = new Migration();
        migration.setGitlabToken(props.gitlab.token);
        migration.setGitlabUrl(props.gitlab.url);
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
