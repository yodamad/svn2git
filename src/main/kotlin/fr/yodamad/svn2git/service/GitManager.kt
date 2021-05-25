package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.io.Shell.isWindows
import fr.yodamad.svn2git.repository.MappingRepository
import fr.yodamad.svn2git.service.util.*
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isEmpty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.charset.Charset.defaultCharset

@Service
open class GitManager(val historyMgr: HistoryManager,
                      val gitCommandManager: GitCommandManager,
                      val repoFormatter: GitRepositoryFormatter,
                      val mappingMgr: MappingManager,
                      val mappingRepository: MappingRepository,
                      var applicationProperties: ApplicationProperties) {

    private val LOG = LoggerFactory.getLogger(GitManager::class.java)
    private val ORIGIN_ALREADY_ADDED = "Origin already added"

    /**
     * Set git config for migration
     */
    open fun setGitConfig(commandManager: CommandManager, workUnit: WorkUnit) {
        // Avoid implicit git gc that may trigger error: fatal: gc is already running on machine '<servername>' pid 124077 (use --force if not)
        // Note: Git GC will be triggered following git svn clone (on large projects) which causes a crash in following steps.
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CONFIG_GLOBAL_GC_AUTO_OFF, "Assure Git Garbage Collection doesn't run in background to avoid conflicts.")
        val gitCommand = "git config --global gc.auto 0"
        execCommand(commandManager, workUnit.directory, gitCommand)
        historyMgr.endStep(history, StatusEnum.DONE, null)

        // Log all git config before
        gitCommandManager.logGitConfig(workUnit)
        gitCommandManager.logUlimit(workUnit)
        // Log default character encoding
        LOG.info("Charset.defaultCharset().displayName(): ${defaultCharset().displayName()}")
    }

    /**
     * Set configuration for GIT local workspace based on application.yml`
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun addDynamicLocalConfig(workUnit: WorkUnit, dynamicLocalConfig: String, dynamicLocalConfigDesc: String) {
        if (StringUtils.isNotEmpty(dynamicLocalConfig) && StringUtils.isNotEmpty(dynamicLocalConfigDesc)) {
            val configParts = dynamicLocalConfig.split(" ").toTypedArray()
            if (configParts.size == 2) {
                val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_DYNAMIC_LOCAL_CONFIG, dynamicLocalConfigDesc)
                LOG.info("Setting Git Config")
                execCommand(workUnit.commandManager, workUnit.directory, setConfig(dynamicLocalConfig))

                LOG.info("Checking Git Config")
                execCommand(workUnit.commandManager, workUnit.directory, readConfig(configParts[0]))
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } else {
                LOG.warn("Problem applying dynamic git local configuration")
            }
        } else {
            LOG.warn("Problem applying dynamic git local configuration")
        }
    }

    /**
     * Git svn clone command to copy svn as git repository
     *
     * @param workUnit Current work unit
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun gitSvnClone(workUnit: WorkUnit) {
        var cloneCommand: String
        val safeCommand: String
        if (!isWindows) {
            if (!isEmpty(workUnit.migration.svnPassword)) {
                val escapedPassword = StringEscapeUtils.escapeJava(workUnit.migration.svnPassword)
                cloneCommand = gitCommandManager.initCommand(workUnit, workUnit.migration.svnUser, escapedPassword)
                safeCommand = gitCommandManager.initCommand(workUnit, workUnit.migration.svnUser, STARS)
            } else if (!isEmpty(applicationProperties.svn.password)) {
                val escapedPassword = StringEscapeUtils.escapeJava(applicationProperties.svn.password)
                cloneCommand = gitCommandManager.initCommand(workUnit, applicationProperties.svn.user, escapedPassword)
                safeCommand = gitCommandManager.initCommand(workUnit, applicationProperties.svn.user, STARS)
            } else {
                cloneCommand = gitCommandManager.initCommand(workUnit, null, null)
                safeCommand = cloneCommand
            }

            // Waiting for Windows support...
            cloneCommand = gitCommandManager.generateGitSvnCloneScript(workUnit, cloneCommand)
        } else {
            gitCommandManager.generateGitSvnClonePackageForWindows(workUnit)
            cloneCommand = "${workUnit.directory}\\gitsvn.ps1 -url ${workUnit.migration.svnUrl} -username ${workUnit.migration.svnUser} -password ${workUnit.migration.svnPassword} -certAcceptResponse t\n"
            safeCommand = cloneCommand
        }

        val history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_CHECKOUT,
            (if (workUnit.commandManager.isFirstAttemptMigration) "" else Constants.REEXECUTION_SKIPPING) + safeCommand)
        // Only Clone if first attempt at migration
        var cloneOK = true
        if (workUnit.commandManager.isFirstAttemptMigration) {
            try {
                execCommand(workUnit.commandManager, workUnit.root, cloneCommand, safeCommand, true)
            } catch (thr: Throwable) {
                thr.printStackTrace()
                LOG.warn("Cannot git svn clone", thr.message)
                var round = 0
                var notOk = true
                while (round++ < applicationProperties.svn.maxFetchAttempts && notOk) {
                    notOk = gitSvnFetch(workUnit, round)
                    gitGC(workUnit, round)
                }
                historyMgr.endStep(history, StatusEnum.FAILED, null)
                throw RuntimeException()
            }
        }
        if (cloneOK) {
            historyMgr.endStep(history, StatusEnum.DONE, null)
        } else {
            historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, null)
        }
    }

    /**
     * Git svn fetch command to copy svn as git repository
     *
     * @param workUnit Current work unit
     * @param round Round number
     * @return if fetch is in failure or not
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun gitSvnFetch(workUnit: WorkUnit, round: Int) : Boolean {
        val fetchCommand = "git svn fetch";

        val history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_FETCH, "$fetchCommand (Round $round)")
        return try {
            execCommand(workUnit.commandManager, workUnit.directory, fetchCommand)
            historyMgr.endStep(history, StatusEnum.DONE, null)
            false
        } catch (thr: Throwable) {
            LOG.error("Cannot git svn fetch", thr.printStackTrace())
            historyMgr.endStep(history, StatusEnum.FAILED, null)
            true
        }
    }

    /**
     * Git gc command to cleanup unnecessary files and optimize the local repository
     *
     * @param workUnit Current work unit
     * @param round Round number
     * @return if fetch is in failure or not
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun gitGC(workUnit: WorkUnit, round: Int) : Boolean {
        val gcCommand = "git gc";

        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_GC, "Round $round : $gcCommand")
        return try {
            execCommand(workUnit.commandManager, workUnit.directory, gcCommand)
            historyMgr.endStep(history, StatusEnum.DONE, null)
            false
        } catch (thr: Throwable) {
            LOG.error("Cannot git gc", thr.printStackTrace())
            historyMgr.endStep(history, StatusEnum.FAILED, null)
            true
        }
    }

    /**
     * Manage trunk and link it to origin
     */
    open fun manageMaster(commandManager: CommandManager, workUnit: WorkUnit, migration: Migration, svn: String) {
        val history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "SVN ${migration.trunk} -> GitLab master")

        // Set origin
        execCommand(commandManager, workUnit.directory,
            gitCommandManager.buildRemoteCommand(workUnit, svn, false),
            gitCommandManager.buildRemoteCommand(workUnit, svn, true))
        if (migration.trunk != "trunk") {
            execCommand(workUnit.commandManager, workUnit.directory, checkoutFromOrigin(migration.trunk))
            execCommand(workUnit.commandManager, workUnit.directory, deleteBranch(MASTER))
            execCommand(workUnit.commandManager, workUnit.directory, renameBranch(MASTER))
        }

        // if no history option set
        if (migration.svnHistory == "nothing") {
            removeHistory(workUnit, MASTER, false, history)
        } else {
            // Push with upstream
            execCommand(commandManager, workUnit.directory, "$GIT_PUSH --set-upstream origin master")
            historyMgr.endStep(history, StatusEnum.DONE)
        }

        // Clean pending file(s) removed by BFG
        execCommand(commandManager, workUnit.directory, resetHard())

        // 5. Apply mappings if some
        val warning = repoFormatter.applyMapping(workUnit, MASTER)
        workUnit.warnings.set(workUnit.warnings.get() || warning)
    }

    /**
     * Remove commit history on a given branch
     *
     * @param workUnit Current work unit
     * @param branch   Branch to work on
     * @param isTag    Flag to check if working on a tag
     * @param history  Current history instance
     */
    open fun removeHistory(workUnit: WorkUnit, branch: String?, isTag: Boolean, history: MigrationHistory?) {
        try {
            LOG.debug("Remove history on $branch")

            // Create new orphan branch and switch to it. The first commit made on this new branch
            // will have no parents and it will be the root of a new history totally disconnected from all the
            // other branches and commits
            execCommand(workUnit.commandManager, workUnit.directory,
                gitCommand(CHECKOUT, "--orphan", "TEMP_BRANCH_$branch"))

            // Stage All (new, modified, deleted) files. Equivalent to git add . (in Git Version 2.x)
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand("add", flags = "-A"))
            try {
                // Create a new commit. Runs git add on any file that is 'tracked' and provide a message
                // for the commit
                execCommand(workUnit.commandManager, workUnit.directory, commitAll("Reset history on $branch"))
            } catch (ex: RuntimeException) {
                // Ignored failed step
            }
            try {
                // Delete (with force) the passed in branch name
                execCommand(workUnit.commandManager, workUnit.directory, deleteBranch(branch!!))
            } catch (ex: RuntimeException) {
                if (ex.message.equals("1", ignoreCase = true)) {
                    // Ignored failed step
                }
            }

            // move/rename a branch and the corresponding reflog
            // (i.e. rename the orphan branch - without history - to the passed in branch name)
            // Note : This fails with exit code 128 (git branch -m tmp_tag) when only folders in the subversion tag.
            // git commit -am above fails because no files
            execCommand(workUnit.commandManager, workUnit.directory, renameBranch(branch!!))

            // i.e. if it is a branch
            if (!isTag) {
                // create the remote
                addRemote(workUnit, true)
                // push to remote
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand("push", "-f", "origin $branch"))
                historyMgr.endStep(history, StatusEnum.DONE, "Push $branch with no history")
            }
        } catch (gitEx: IOException) {
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        } catch (gitEx: InterruptedException) {
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        }
    }

    /**
     * Add remote url to git folder
     *
     * @param workUnit  Current work unit
     * @param trunkOnly Only check trunk or not
     */
    open fun addRemote(workUnit: WorkUnit, trunkOnly: Boolean) {
        if (workUnit.migration.trunk == null && (trunkOnly || workUnit.migration.branches == null)) {
            try {
                // Set origin
                execCommand(workUnit.commandManager, workUnit.directory,
                    gitCommandManager.buildRemoteCommand(workUnit, null, false),
                    gitCommandManager.buildRemoteCommand(workUnit, null, true))
            } catch (rEx: IOException) {
                LOG.debug(ORIGIN_ALREADY_ADDED)
                // Skip
                // TODO : see to refactor, that's pretty ugly
            } catch (rEx: InterruptedException) {
                LOG.debug(ORIGIN_ALREADY_ADDED)
            } catch (rEx: RuntimeException) {
                LOG.debug(ORIGIN_ALREADY_ADDED)
            }
        }
    }
}
