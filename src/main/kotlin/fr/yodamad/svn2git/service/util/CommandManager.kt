package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.domain.Migration
import org.apache.commons.lang3.StringUtils
import java.util.*

class CommandManager(val migration: Migration) {
    var rootDir = ""
    /**
     * This is set to true when the svn clone step (and svn cleanup) has finished.
     */
    var isReexecutable = false
    // The trace of the commands executed in the current thread
    var commandLog = LinkedHashMap<String, String>()
    /**
     * Object tracing commandError
     */
    var commandError: CommandError = CommandError()
    val isFirstAttemptMigration: Boolean
        get() = StringUtils.isBlank(migration.workingDirectory)
    val workingDirectoryPath: String
        get() = migration.workingDirectory

    /**
     * Trace successful commands executed through execCommand
     *
     * @param directory
     * @param securedCommandToPrint
     */
    fun addSuccessfulCommand(directory: String, securedCommandToPrint: String) {
        commandLog[commandLog.size.toString() + "_" + directory] = securedCommandToPrint
    }

    fun addFailedCommand(directory: String, securedCommandToPrint: String, stderr: String) {

        // commandError not persisted for the moment
        commandError.directory = directory
        commandError.command = securedCommandToPrint
        commandError.error = stderr
    }

    inner class CommandError {
        var directory = ""
        var command = ""
        var error = ""
        fun isError(): Boolean {
            return StringUtils.isNotBlank(error)
        }
    }
}
