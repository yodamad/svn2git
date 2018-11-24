package fr.yodamad.svn2git.domain;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Work unit
 */
public class WorkUnit {

    public Migration migration;
    public String root;
    public String directory;
    public AtomicBoolean warnings;

    public WorkUnit(Migration migration, String root, String directory, AtomicBoolean warnings) {
        this.migration = migration;
        this.root = root;
        this.directory = directory;
        this.warnings = warnings;
    }
}
