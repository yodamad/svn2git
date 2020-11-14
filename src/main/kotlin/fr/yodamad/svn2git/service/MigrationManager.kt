package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.domain.enumeration.SvnLayout
import fr.yodamad.svn2git.functions.listRemotes
import fr.yodamad.svn2git.repository.MigrationHistoryRepository
import fr.yodamad.svn2git.repository.MigrationRepository
import fr.yodamad.svn2git.service.util.CommandManager
import fr.yodamad.svn2git.service.util.MarkdownGenerator
import fr.yodamad.svn2git.service.util.MigrationConstants
import fr.yodamad.svn2git.service.util.Shell
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Component
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors
import kotlin.NoSuchElementException

@Component
open class MigrationManager(val cleaner: Cleaner,
                            val gitManager: GitManager,
                            val historyMgr: HistoryManager,
                            val migrationRepository: MigrationRepository,
                            val migrationHistoryRepository: MigrationHistoryRepository,
                            val applicationProperties: ApplicationProperties,
                            val markdownGenerator: MarkdownGenerator) {

    private val LOG = LoggerFactory.getLogger(MigrationManager::class.java)

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
        val workUnit = WorkUnit(migration, Shell.formatDirectory(rootDir),
            Shell.gitWorkingDir(rootDir, migration.svnGroup), AtomicBoolean(false), commandManager)
        try {

            // Start migration
            migration.status = StatusEnum.RUNNING
            migrationRepository.save(migration)

            // 1. Create project on gitlab : OK
            gitManager.createGitlabProject(migration)

            // If reexecution we initialise from clean copy.
            initRootDirectoryFromCopy(workUnit)

            // 2. Checkout empty repository : OK
            val svn: String = initDirectory(workUnit)

            // 2.1 Avoid implicit git gc that may trigger error: fatal: gc is already running on machine '<servername>' pid 124077 (use --force if not)
            //     Note: Git GC will be triggered following git svn clone (on large projects) which causes a crash in following steps.
            history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CONFIG_GLOBAL_GC_AUTO_OFF, "Assure Git Garbage Collection doesn't run in background to avoid conflicts.")
            gitCommand = "git config --global gc.auto 0"
            Shell.execCommand(commandManager, workUnit.directory, gitCommand)
            historyMgr.endStep(history, StatusEnum.DONE, null)

            // Log all git config before
            logGitConfig(workUnit)
            logUlimit(workUnit)
            // Log default character encoding
            LOG.info("Charset.defaultCharset().displayName():" + Charset.defaultCharset().displayName())

            // 2.2. SVN checkout
            gitManager.gitSvnClone(workUnit)
            checkGitConfig(workUnit)
            copyRootDirectory(workUnit)
            // Migration is now reexecutable in cases where there is a failure
            commandManager.isReexecutable = true

            // Apply dynamic local configuration
            applicationProperties.getGitlab().getDynamicLocalConfig().stream().map { s: String -> s.split(",").toTypedArray() }.collect(Collectors.toMap({ a: Array<String> -> a[0].trim { it <= ' ' } }, { a: Array<String> -> a[1].trim { it <= ' ' } })).forEach { (key: String?, value: String?) ->
                try {
                    addDynamicLocalConfig(workUnit, key, value)
                } catch (e: IOException) {
                    LOG.error(e.message, e)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw RuntimeException(e)
                }
            }

            // 2.3. Remove phantom branches
            if (migration.branches != null) {
                history = historyMgr.startStep(migration, StepEnum.BRANCH_CLEAN, "Clean removed SVN branches")
                val pairInfo = cleaner.cleanRemovedElements(workUnit, false)
                if (pairInfo!!.first.get()) {
                    //  Some branches have failed
                    historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, String.format("Failed to remove branches %s", pairInfo.second))
                } else {
                    historyMgr.endStep(history, StatusEnum.DONE, null)
                }
            }

            // 2.4. Remove phantom tags
            if (migration.tags != null) {
                history = historyMgr.startStep(migration, StepEnum.TAG_CLEAN, "Clean removed SVN tags")
                val pairInfo = cleaner.cleanRemovedElements(workUnit, true)
                if (pairInfo!!.first.get()) {
                    //  Some branches have failed
                    historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, String.format("Failed to remove tags %s", pairInfo.second))
                } else {
                    historyMgr.endStep(history, StatusEnum.DONE, null)
                }
            }

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
                    gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive"
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                    gitCommand = "git reset HEAD"
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                    gitCommand = "git clean -fd"
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                }

                // 4. Git push master based on SVN trunk
                if (migration.trunk != null) {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, String.format("SVN %s -> GitLab master", migration.trunk))

                    // Set origin
                    Shell.execCommand(commandManager, workUnit.directory,
                        gitManager.buildRemoteCommand(workUnit, svn, false),
                        gitManager.buildRemoteCommand(workUnit, svn, true))
                    if (migration.trunk != "trunk") {
                        gitCommand = String.format("git checkout -b %s %s", migration.trunk, "refs/remotes/origin/" + migration.trunk)
                        Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                        gitCommand = String.format("git branch -D master")
                        Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                        gitCommand = String.format("git branch -m master")
                        Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                    }

                    // if no history option set
                    if (migration.svnHistory == "nothing") {
                        gitManager.removeHistory(workUnit, MigrationConstants.MASTER, false, history)
                    } else {
                        // Push with upstream
                        gitCommand = String.format("%s --set-upstream origin master", MigrationConstants.GIT_PUSH)
                        Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                        historyMgr.endStep(history, StatusEnum.DONE, null)
                    }

                    // Clean pending file(s) removed by BFG
                    gitCommand = "git reset --hard origin/master"
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)

                    // 5. Apply mappings if some
                    val warning = gitManager.applyMapping(workUnit, MigrationConstants.MASTER)
                    workUnit.warnings.set(workUnit.warnings.get() || warning)
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, migration.trunk)
                    historyMgr.endStep(history, StatusEnum.IGNORED, String.format("Skip %s", migration.trunk))
                }

                // 6. List branches & tags
                val remotes: List<String> = listRemotes(workUnit.directory)
                // Extract branches
                if (!StringUtils.isEmpty(migration.branches) && migration.branches == "*") {
                    gitManager.manageBranches(workUnit, remotes)
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Branches")
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip branches")
                }

                // Extract tags
                if (!StringUtils.isEmpty(migration.tags) && migration.tags == "*") {
                    gitManager.manageTags(workUnit, remotes)
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Tags")
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip tags")
                }

                // Generate summary
                try {
                    history = historyMgr.startStep(migration, StepEnum.README_MD, "Generate README.md to summarize migration")
                    gitCommand = "git checkout master"
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)

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
                        Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                    }
                    historyMgr.endStep(history, StatusEnum.DONE, null)
                    historyMgr.forceFlush()
                    markdownGenerator.generateSummaryReadme(historyMgr.loadMigration(workUnit.migration.id), cleanedFilesManager, workUnit)
                    gitCommand = "git add README.md"
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                    gitCommand = "git commit -m \"📃 Add generated README.md\""
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                    gitCommand = String.format("%s --set-upstream origin master", MigrationConstants.GIT_PUSH)
                    Shell.execCommand(commandManager, workUnit.directory, gitCommand)
                    historyMgr.endStep(history, StatusEnum.DONE, null)
                } catch (exc: Exception) {
                    historyMgr.endStep(history, StatusEnum.FAILED, exc.message)
                }
            } else {
                history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, String.format("%s, Tags, Branches", migration.trunk))
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
            logGitConfig(workUnit)
        } catch (exc: Throwable) {
            history = migrationHistoryRepository.findFirstByMigration_IdOrderByIdDesc(migrationId)
            if (history != null) {
                LOG.error("Failed step : " + history.step, exc)
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
            StepEnum.CLEANING, String.format("Remove %s", folderToDelete))

        // Sanity check
        if (!StringUtils.isBlank(folderToDelete) && folderToDelete.contains("20") && folderToDelete.contains("_")) {
            try {
                if (Shell.isWindows) {
                    // FileUtils.deleteDirectory(new File(folderToDelete));
                    // JBU : Fails occassionally on windows with file lock issue
                    // FileUtils.deleteDirectory(new File(folderToDelete));
                    // JBU : Fails on windows not able to delete a large number of files?..
                    val gitCommand = String.format("rd /s /q %s", folderToDelete)
                    Shell.execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand)
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
                // /J Copy using unbuffered I/O. Recommended for very large files.
                String.format("Xcopy /E /I /H /Q %s %s_copy", workUnit.root, workUnit.root)
            } else {
                // cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
                // root has no trailling / e.g. folder_12345
                String.format("cp -a %s %s_copy", workUnit.root, workUnit.root)
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
            var gitCommand : String = if (Shell.isWindows) {
                // /J Copy using unbuffered I/O. Recommended for very large files.
                String.format("Xcopy /E /I /H /Q %s_copy %s", workUnit.root, workUnit.root)
            } else {
                // cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
                // root has no trailling / e.g. folder_12345
                String.format("cp -a %s_copy %s", workUnit.root, workUnit.root)
            }
            Shell.execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand)

            // git reset incase a deployment has changed permissions
            // deployment of application seems to change files from 644 to 755 which is not desired.
            gitCommand = "git reset --hard HEAD"
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            historyMgr.endStep(history, StatusEnum.DONE, null)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    open fun logGitConfig(workUnit: WorkUnit) {
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_SHOW_CONFIG, "Log Git Config and origin of config.")
        val gitCommand = "git config --list --show-origin"
        Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
        historyMgr.endStep(history, StatusEnum.DONE, null)
    }

    @Throws(IOException::class, InterruptedException::class)
    open fun checkGitConfig(workUnit: WorkUnit) {
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_SET_CONFIG, "Log Git Config and origin of config.")
        var gitCommand = "git config user.name"
        //String workDir = format("%s/%s", workUnit.directory, workUnit.migration.getSvnProject());
        try {
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
        } catch (rEx: RuntimeException) {
            LOG.info("Git user.email and user.name not set, use default values based on gitlab user set in UI")
            gitCommand = String.format("git config user.email %s@svn2git.fake", workUnit.migration.user)
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            gitCommand = String.format("git config user.name %s", workUnit.migration.user)
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
        } finally {
            historyMgr.endStep(history, StatusEnum.DONE, null)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    open fun logUlimit(workUnit: WorkUnit) {

        // On linux servers trace what ulimit value is
        if (!Shell.isWindows) {
            val history = historyMgr.startStep(workUnit.migration, StepEnum.ULIMIT, "Show Ulimit -u value.")
            try {
                val command = "ulimit -u"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, command)
            } catch (exc: java.lang.Exception) {
                // Ignore exception as it's just info displayed
            } finally {
                historyMgr.endStep(history, StatusEnum.DONE, null)
            }
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
                var gitCommand = "git config $dynamicLocalConfig"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                //display value after
                LOG.info("Checking Git Config")
                gitCommand = "git config " + configParts[0]
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
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
        val svn = if (StringUtils.isEmpty(workUnit.migration.svnProject)) workUnit.migration.svnGroup else workUnit.migration.svnProject
        if (workUnit.commandManager.isFirstAttemptMigration) {
            val mkdir: String
            if (Shell.isWindows) {
                var path = if (workUnit.directory.startsWith("/")) String.format("%s%s", System.getenv("SystemDrive"), workUnit.directory) else workUnit.directory
                path = path.replace("/".toRegex(), "\\\\")
                mkdir = String.format("mkdir %s", path)
            } else {
                mkdir = String.format("mkdir -p %s", workUnit.directory)
            }
            Shell.execCommand(workUnit.commandManager, applicationProperties.work.directory, mkdir)
        }
        return svn
    }
}
