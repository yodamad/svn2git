package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.service.HistoryManager
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.IOException

@Service
open class IOManager(val historyMgr: HistoryManager,
                     val applicationProperties: ApplicationProperties) {

    private val LOG = LoggerFactory.getLogger(IOManager::class.java)

    /**
     * Delete working directory.
     * Can be in context of usual cleanup.
     * Can be in context of failed migration (in preparation for clean initialisation next time)
     *
     * @param workUnit
     * @param copy boolean indicating whether to delete the copy or not
     */
    open fun deleteWorkingRoot(workUnit: WorkUnit, copy: Boolean) {

        // Construct folder name (will be copy or not)
        val folderToDelete = String.format("%s%s", workUnit.root, if (copy) "_copy" else "")

        // 7. Clean work directory
        val history = historyMgr.startStep(workUnit.migration,
            StepEnum.CLEANING, "Remove $folderToDelete")

        // Sanity check
        if (!StringUtils.isBlank(folderToDelete) && folderToDelete.contains("20") && folderToDelete.contains("_")) {
            try {
                if (Shell.isWindows) {
                    // FileUtils.deleteDirectory(new File(folderToDelete));
                    // Fails occassionally on windows with file lock issue
                    // Fails on windows not able to delete a large number of files?..
                    Shell.execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), "rd /s /q $folderToDelete")
                } else {
                    // Seems to work ok on linux. Keeping Java command for the moment
                    FileSystemUtils.deleteRecursively(File(folderToDelete))
                }
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } catch (exc: java.lang.Exception) {
                LOG.error("Failed deleteDirectory: ", exc)
                historyMgr.endStep(history, StatusEnum.FAILED, exc.message)
            }
        } else {
            LOG.error("Failed deleteDirectory: Badly formed delete path")
            historyMgr.endStep(history, StatusEnum.FAILED, "Badly formed delete path")
        }
    }

    /**
     * When Git Svn Clone step is completed (and associated cleanup)
     * we copy the filesystem so that the migration can be reexecuted from this clean state.
     * This avoids waiting for lengthy Git svn clone step
     *
     * @param workUnit
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun copyRootDirectory(workUnit: WorkUnit) {
        val history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_COPY_ROOT_FOLDER,
            (if (workUnit.commandManager.isFirstAttemptMigration) "" else Constants.REEXECUTION_SKIPPING) +
                "Copying Root Folder")
        if (workUnit.commandManager.isFirstAttemptMigration) {
            val gitCommand: String = if (Shell.isWindows) {
                "$WIN_COPY_DIR ${workUnit.root} ${workUnit.root}_copy"
            } else {
                "$COPY_DIR ${workUnit.root} ${workUnit.root}_copy"
            }
            Shell.execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand)
        }
        historyMgr.endStep(history, StatusEnum.DONE, null)
    }

    /**
     * In a migration reexectution the first step is to recuperate a clean state.
     *
     * @param workUnit
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun initRootDirectoryFromCopy(workUnit: WorkUnit) {
        if (!workUnit.commandManager.isFirstAttemptMigration) {
            val history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_COPY_ROOT_FOLDER,
                (if (workUnit.commandManager.isFirstAttemptMigration) "" else Constants.REEXECUTION_SKIPPING) +
                    "Initialising Root Directory from Copy in context of migration reexecution.")

            // The clean copy folder is used to reinitialise the workUnit.root Folder
            val gitCommand : String = if (Shell.isWindows) {
                "$WIN_COPY_DIR ${workUnit.root}_copy ${workUnit.root}"
            } else {
                "$COPY_DIR ${workUnit.root}_copy ${workUnit.root}"
            }
            Shell.execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand)

            // git reset incase a deployment has changed permissions
            // deployment of application seems to change files from 644 to 755 which is not desired.
            Shell.execCommand(workUnit.commandManager, workUnit.directory, resetHead())
            historyMgr.endStep(history, StatusEnum.DONE)
        }
    }
}
