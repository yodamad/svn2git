package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.domain.Migration;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;

public class CommandManager {

    // The current migration
    private final Migration migration;

    private String rootDir = "";

    /**
     * This is set to true when the svn clone step (and svn cleanup) has finished.
     */
    private boolean reexecutable = false;

    // The trace of the commands executed in the current thread
    private LinkedHashMap<String, String> commandLog = new LinkedHashMap<>();

    /**
     * Object tracing commandError
     */
    private CommandError commandError = new CommandError();

    /**
     * Constructor
     *
     * @param migration
     */
    public CommandManager(Migration migration) {
        this.migration = migration;
    }

    /**
     * Gets commandError
     *
     * @return value of commandError
     */
    public CommandError getCommandError() {
        return commandError;
    }

    /**
     * Set the commandError.
     *
     * @param commandError
     */
    public void setCommandError(CommandError commandError) {
        this.commandError = commandError;
    }

    /**
     * Gets reexecutable
     *
     * @return value of reexecutable
     */
    public boolean isReexecutable() {
        return reexecutable;
    }

    /**
     * Set the reexecutable.
     *
     * @param reexecutable
     */
    public void setReexecutable(boolean reexecutable) {
        this.reexecutable = reexecutable;
    }

    public boolean isFirstAttemptMigration() {
        return StringUtils.isBlank(migration.getWorkingDirectory());
    }

    /**
     * Gets migration
     *
     * @return value of migration
     */
    public Migration getMigration() {
        return migration;
    }

    /**
     * Gets commandLog
     *
     * @return value of commandLog
     */
    public LinkedHashMap<String, String> getCommandLog() {
        return commandLog;
    }

    /**
     * Set the commandLog.
     *
     * @param commandLog
     */
    public void setCommandLog(LinkedHashMap<String, String> commandLog) {
        this.commandLog = commandLog;
    }

    /**
     * Gets rootDir
     *
     * @return value of rootDir
     */
    public String getRootDir() {
        return rootDir;
    }

    /**
     * Set the rootDir.
     *
     * @param rootDir
     */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    public String getWorkingDirectoryPath() {
        return migration.getWorkingDirectory();
    }

    /**
     * Trace successful commands executed through execCommand
     *
     * @param directory
     * @param securedCommandToPrint
     */
    public void addSuccessfulCommand(String directory, String securedCommandToPrint) {
        this.commandLog.put(this.commandLog.size() + "_" + directory, securedCommandToPrint);
    }

    public void addFailedCommand(String directory, String securedCommandToPrint, String stderr) {

        // commandError not persisted for the moment
        commandError.directory = directory;
        commandError.command = securedCommandToPrint;
        commandError.error = stderr;

    }

    public class CommandError {
        String directory = "";
        String command = "";
        String error = "";

        public boolean isError() {
            return StringUtils.isNotBlank(error);
        }
    }

}
