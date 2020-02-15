package fr.yodamad.svn2git.domain;

import fr.yodamad.svn2git.service.util.CommandManager;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Work unit
 */
public class WorkUnit {

    public Migration migration;
    public String root;
    public String directory;
    public AtomicBoolean warnings;
    public CommandManager commandManager;

    public WorkUnit(Migration migration, String root, String directory, AtomicBoolean warnings, CommandManager commandManager) {

        this.migration = migration;
        this.root = root;
        this.directory = directory;
        this.warnings = warnings;
        this.commandManager = commandManager;
    }
}
