package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.domain.enumeration.SvnLayout
import fr.yodamad.svn2git.functions.listRemotes
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.io.Shell.formatDirectory
import fr.yodamad.svn2git.io.Shell.isWindows
import fr.yodamad.svn2git.repository.MigrationHistoryRepository
import fr.yodamad.svn2git.repository.MigrationRepository
import fr.yodamad.svn2git.service.util.*
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isEmpty
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Component
import org.springframework.util.FileSystemUtils.deleteRecursively
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors.toMap
import kotlin.NoSuchElementException

@Component
open class MigrationManager(val cleaner: Cleaner,
                            val gitManager: GitManager,
                            val gitBranchManager: GitBranchManager,
                            val gitTagManager: GitTagManager,
                            val gitCommandManager: GitCommandManager,
                            val gitlabManager: GitlabManager,
                            val historyMgr: HistoryManager,
                            val migrationRepository: MigrationRepository,
                            val migrationHistoryRepository: MigrationHistoryRepository,
                            val applicationProperties: ApplicationProperties,
                            val markdownGenerator: MarkdownGenerator) {

    companion object {
        private val LOG = LoggerFactory.getLogger(MigrationManager::class.java)
    }

    private val FAILED_DIR = "Failed to create directory : %s"

    /**
     * Start a migration in a dedicated thread
     *
     * @param migrationId ID for migration to start
     * @param retry       Flag to know if it's the first attempt or a retry
     */
    @Async
    open fun startMigration(migrationId: Long, retry: Boolean): Future<String>? {
        var gitCommand: String
        val migration = migrationRepository.findById(migrationId).orElseThrow { NoSuchElementException() }
        var history: MigrationHistory? = null
        val rootDir: String
        val commandManager = CommandManager(migration)
        try {
            history = historyMgr.startStep(migration, StepEnum.INIT,
                (if (commandManager.isFirstAttemptMigration) "" else Constants.REEXECUTION_SKIPPING) + "Create working directory")
            // If migration.workingDirectory is set we are reexecuting a 'partial' migration
            rootDir = if (commandManager.isFirstAttemptMigration) {
                Shell.workingDir(commandManager, applicationProperties.work.directory, migration)
            } else {
                commandManager.workingDirectoryPath
            }
            historyMgr.endStep(history, StatusEnum.DONE, null)
        } catch (ex: IOException) {
            historyMgr.endStep(history, StatusEnum.FAILED, String.format(FAILED_DIR, ex.message))
            migration.status = StatusEnum.FAILED
            migrationRepository.save(migration)
            return AsyncResult("KO")
        } catch (ex: InterruptedException) {
            historyMgr.endStep(history, StatusEnum.FAILED, String.format(FAILED_DIR, ex.message))
            migration.status = StatusEnum.FAILED
            migrationRepository.save(migration)
            return AsyncResult("KO")
        } catch (ex: RuntimeException) {
            historyMgr.endStep(history, StatusEnum.FAILED, String.format(FAILED_DIR, ex.message))
            migration.status = StatusEnum.FAILED
            migrationRepository.save(migration)
            return AsyncResult("KO")
        }
        val workUnit = WorkUnit(migration, formatDirectory(rootDir),
            Shell.gitWorkingDir(rootDir, migration.svnGroup), AtomicBoolean(false), commandManager)
        try {

            // Start migration
            migration.status = StatusEnum.RUNNING
            migrationRepository.save(migration)

            // 1. Create project on gitlab : OK
            gitlabManager.createGitlabProject(migration)

            // If reexecution we initialise from clean copy.
            initRootDirectoryFromCopy(workUnit)

            // 2. Checkout empty repository : OK
            val svn: String = initDirectory(workUnit)

            // 2.1 Set some git config
            gitManager.setGitConfig(commandManager, workUnit)

            // 2.2. SVN checkout
            gitManager.gitSvnClone(workUnit)
            checkGitConfig(workUnit)
            copyRootDirectory(workUnit)
            // Migration is now reexecutable in cases where there is a failure
            commandManager.isReexecutable = true

            // Apply dynamic local configuration
            applicationProperties.getGitlab().getDynamicLocalConfig().stream()
                .map { s: String -> s.split(",").toTypedArray() }
                .collect(toMap({ a: Array<String> -> a[0].trim { it <= ' ' } }, { a: Array<String> -> a[1].trim { it <= ' ' } }))
                .forEach { (key: String?, value: String?) ->
                    try {
                        addDynamicLocalConfig(workUnit, key, value)
                    } catch (e: IOException) {
                        LOG.error(e.message, e)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw RuntimeException(e)
                    }
                }

            // 2.3. Remove phantom elements
            cleaner.cleanElementsOn(workUnit, false)
            cleaner.cleanElementsOn(workUnit, true)

            // 3. Clean files
            // 3.1 List files to remove. uploads binaries to Artifactory.
            val cleanedFilesManager = cleaner.listCleanedFiles(workUnit)
            LOG.info(cleanedFilesManager.toString())

            // Only launch clean steps if there is a file to clean in trunk, branches or tags.
            // If no files at this stage, no migration is executed i.e. no push to gitlab.
            if (cleanedFilesManager!!.existsFileInSvnLayout(true, SvnLayout.ALL)) {

                // 3.2 Remove
                val cleanFolderWithBFG = cleaner.cleanFolderWithBFG(workUnit)
                val cleanExtensions = cleaner.cleanForbiddenExtensions(workUnit)
                val cleanLargeFiles = cleaner.cleanLargeFiles(workUnit)
                if (cleanExtensions || cleanLargeFiles || cleanFolderWithBFG) {
                    gitManager.gitClean(commandManager, workUnit)
                }

                // 4. Git push master based on SVN trunk
                if (migration.trunk != null) {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, String.format("SVN %s -> GitLab master", migration.trunk))

                    // Set origin
                    execCommand(commandManager, workUnit.directory,
                        gitCommandManager.buildRemoteCommand(workUnit, svn, false),
                        gitCommandManager.buildRemoteCommand(workUnit, svn, true))
                    if (migration.trunk != "trunk") {
                        gitCommand = "git checkout -b ${migration.trunk} refs/remotes/origin/${migration.trunk}"
                        execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                        execCommand(workUnit.commandManager, workUnit.directory, "git branch -D master")
                        execCommand(workUnit.commandManager, workUnit.directory, "git branch -m master")
                    }

                    // if no history option set
                    if (migration.svnHistory == "nothing") {
                        gitManager.removeHistory(workUnit, MASTER, false, history)
                    } else {
                        // Push with upstream
                        execCommand(commandManager, workUnit.directory, "$GIT_PUSH --set-upstream origin master")
                        historyMgr.endStep(history, StatusEnum.DONE)
                    }

                    // Clean pending file(s) removed by BFG
                    execCommand(commandManager, workUnit.directory, "git reset --hard origin/master")

                    // 5. Apply mappings if some
                    val warning = gitManager.applyMapping(workUnit, MASTER)
                    workUnit.warnings.set(workUnit.warnings.get() || warning)
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, migration.trunk)
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip ${migration.trunk}")
                }

                // 6. List branches & tags
                val remotes: List<String> = listRemotes(workUnit.directory)
                // Extract branches
                if (!isEmpty(migration.branches) && migration.branches == "*") {
                    gitBranchManager.manageBranches(workUnit, remotes)
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Branches")
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip branches")
                }

                // Extract tags
                if (!isEmpty(migration.tags) && migration.tags == "*") {
                    gitTagManager.manageTags(workUnit, remotes)
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Tags")
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip tags")
                }

                // Generate summary
                try {
                    history = historyMgr.startStep(migration, StepEnum.README_MD, "Generate README.md to summarize migration")
                    execCommand(commandManager, workUnit.directory, "git checkout master")

                    // If master not migrated, clean it to add only README.md
                    if (migration.trunk == null) {
                        Arrays.stream(File(workUnit.directory).listFiles())
                            .filter { f: File -> !f.name.equals(".git", ignoreCase = true) }
                            .forEach { f: File? ->
                                try {
                                    FileUtils.forceDelete(f)
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        gitCommand = "git commit -am \"Clean master not migrated to add future REAMDE.md\""
                        execCommand(commandManager, workUnit.directory, gitCommand)
                    }
                    historyMgr.endStep(history, StatusEnum.DONE)
                    historyMgr.forceFlush()
                    markdownGenerator.generateSummaryReadme(historyMgr.loadMigration(workUnit.migration.id), cleanedFilesManager, workUnit)
                    execCommand(commandManager, workUnit.directory, "git add README.md")
                    execCommand(commandManager, workUnit.directory, "git commit -m \"📃 Add generated README.md\"")
                    execCommand(commandManager, workUnit.directory, "$GIT_PUSH --set-upstream origin master")
                    historyMgr.endStep(history, StatusEnum.DONE)
                } catch (exc: Exception) {
                    historyMgr.endStep(history, StatusEnum.FAILED, exc.message)
                }
            } else {
                history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "${migration.trunk}, Tags, Branches")
                historyMgr.endStep(history, StatusEnum.IGNORED, "Skipping Migration : No Files Available. No Push to Gitlab")
            }

            // Finalize migration
            if (workUnit.warnings.get()) {
                migration.status = StatusEnum.DONE_WITH_WARNINGS
            } else {
                migration.status = StatusEnum.DONE
            }

            // migration was successful assure workingDirectory is set to empty so no reexecution is possible
            migration.workingDirectory = ""
            deleteWorkingRoot(workUnit, true)
            migrationRepository.save(migration)

            // Log all git config after operations
            gitCommandManager.logGitConfig(workUnit)
        } catch (exc: Throwable) {
            history = migrationHistoryRepository.findFirstByMigration_IdOrderByIdDesc(migrationId)
            if (history != null) {
                LOG.error("Failed step : ${history.step}", exc)
                historyMgr.endStep(history, StatusEnum.FAILED, exc.message)
            }

            // A copy has been made and an error thrown. We can reexecute next time.
            if (commandManager.isReexecutable) {
                migration.workingDirectory = rootDir
                deleteWorkingRoot(workUnit, false)
                LOG.info("Deleting working directory")
                LOG.info("REASON:commandManager.isReexecutable() AND ERROR during migration")
            }
            migration.status = StatusEnum.FAILED
            migrationRepository.save(migration)
        } finally {
            LOG.debug("=====           Commands Executed          =======")
            LOG.debug("==================================================")
            commandManager.commandLog.forEach { (k: String, v: String) -> LOG.debug("Directory : $k Command : $v") }
            LOG.debug("==================================================")
            if (applicationProperties.getFlags().getCleanupWorkDirectory()) {
                // TODO : handle case where already deleted due to migration failure. See above.
                deleteWorkingRoot(workUnit, false)
            } else {
                LOG.info("Not cleaning up working directory")
                LOG.info("REASON:applicationProperties.getFlags().getCleanupWorkDirectory()==True")
            }
            LOG.info(String.format("Migration from SVN (%s) %s to Gitlab %s group completed with status %s",
                migration.svnGroup, migration.svnProject,
                migration.gitlabGroup,
                migration.status))
        }
        return AsyncResult("THE_END")
    }

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
        if (!isBlank(folderToDelete) && folderToDelete.contains("20") && folderToDelete.contains("_")) {
            try {
                if (isWindows) {
                    // FileUtils.deleteDirectory(new File(folderToDelete));
                    // Fails occassionally on windows with file lock issue
                    // Fails on windows not able to delete a large number of files?..
                    execCommand(workUnit.commandManager, formatDirectory(applicationProperties.work.directory), "rd /s /q $folderToDelete")
                } else {
                    // Seems to work ok on linux. Keeping Java command for the moment
                    deleteRecursively(File(folderToDelete))
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
            val gitCommand: String = if (isWindows) {
                "Xcopy /E /I /H /Q ${workUnit.root} ${workUnit.root}_copy"
            } else {
                // cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
                // root has no trailling / e.g. folder_12345
                "cp -a ${workUnit.root} ${workUnit.root}_copy"
            }
            execCommand(workUnit.commandManager, formatDirectory(applicationProperties.work.directory), gitCommand)
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
            val gitCommand : String = if (isWindows) {
                // /J Copy using unbuffered I/O. Recommended for very large files.
                "Xcopy /E /I /H /Q ${workUnit.root}_copy ${workUnit.root}"
            } else {
                // cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
                // root has no trailling / e.g. folder_12345
                "cp -a ${workUnit.root}_copy ${workUnit.root}"
            }
            execCommand(workUnit.commandManager, formatDirectory(applicationProperties.work.directory), gitCommand)

            // git reset incase a deployment has changed permissions
            // deployment of application seems to change files from 644 to 755 which is not desired.
            execCommand(workUnit.commandManager, workUnit.directory, "git reset --hard HEAD")
            historyMgr.endStep(history, StatusEnum.DONE)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    open fun checkGitConfig(workUnit: WorkUnit) {
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_SET_CONFIG, "Log Git Config and origin of config.")
        try {
            execCommand(workUnit.commandManager, workUnit.directory, "git config user.name")
        } catch (rEx: RuntimeException) {
            LOG.info("Git user.email and user.name not set, use default values based on gitlab user set in UI")
            execCommand(workUnit.commandManager, workUnit.directory, "git config user.email ${workUnit.migration.user}@svn2git.fake")
            execCommand(workUnit.commandManager, workUnit.directory, "git config user.name ${workUnit.migration.user}")
        } finally {
            historyMgr.endStep(history, StatusEnum.DONE)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    open fun addDynamicLocalConfig(workUnit: WorkUnit, dynamicLocalConfig: String, dynamicLocalConfigDesc: String) {
        if (StringUtils.isNotEmpty(dynamicLocalConfig) && StringUtils.isNotEmpty(dynamicLocalConfigDesc)) {
            val configParts = dynamicLocalConfig.split(" ").toTypedArray()
            if (configParts.size == 2) {
                val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_DYNAMIC_LOCAL_CONFIG, dynamicLocalConfigDesc)
                LOG.info("Setting Git Config")
                // apply new local config
                execCommand(workUnit.commandManager, workUnit.directory, "git config $dynamicLocalConfig")

                //display value after
                LOG.info("Checking Git Config")
                execCommand(workUnit.commandManager, workUnit.directory, "git config ${configParts[0]}")
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } else {
                LOG.warn("Problem applying dynamic git local configuration!!!")
            }
        } else {
            LOG.warn("Problem applying dynamic git local configuration!!!")
        }
    }

    /**
     * Create empty repository
     *
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun initDirectory(workUnit: WorkUnit): String {
        val svn = if (isEmpty(workUnit.migration.svnProject) && isEmpty(workUnit.migration.gitlabProject)) workUnit.migration.svnGroup else workUnit.migration.gitlabProject
        if (workUnit.commandManager.isFirstAttemptMigration) {
            val mkdir: String
            if (isWindows) {
                var path = if (workUnit.directory.startsWith("/")) String.format("%s%s", System.getenv("SystemDrive"), workUnit.directory) else workUnit.directory
                path = path.replace("/".toRegex(), "\\\\")
                mkdir = "mkdir $path"
            } else {
                mkdir = "mkdir -p ${workUnit.directory}"
            }
            execCommand(workUnit.commandManager, applicationProperties.work.directory, mkdir)
        }
        return svn
    }
}
