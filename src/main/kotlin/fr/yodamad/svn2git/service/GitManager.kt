package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.io.Shell.execCommand
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

    @Throws(IOException::class, InterruptedException::class)
    open fun addDynamicLocalConfig(workUnit: WorkUnit, dynamicLocalConfig: String, dynamicLocalConfigDesc: String) {
        if (StringUtils.isNotEmpty(dynamicLocalConfig) && StringUtils.isNotEmpty(dynamicLocalConfigDesc)) {
            val configParts = dynamicLocalConfig.split(" ").toTypedArray()
            if (configParts.size == 2) {
                val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_DYNAMIC_LOCAL_CONFIG, dynamicLocalConfigDesc)
                LOG.info("Setting Git Config")
                // apply new local config
                execCommand(workUnit.commandManager, workUnit.directory, setConfig(dynamicLocalConfig))

                //display value after
                LOG.info("Checking Git Config")
                execCommand(workUnit.commandManager, workUnit.directory, readConfig(configParts[0]))
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } else {
                LOG.warn("Problem applying dynamic git local configuration!!!")
            }
        } else {
            LOG.warn("Problem applying dynamic git local configuration!!!")
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
        val cloneCommand: String
        val safeCommand: String
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
        val history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_CHECKOUT,
            (if (workUnit.commandManager.isFirstAttemptMigration) "" else Constants.REEXECUTION_SKIPPING) + safeCommand)
        // Only Clone if first attempt at migration
        if (workUnit.commandManager.isFirstAttemptMigration) {
            execCommand(workUnit.commandManager, workUnit.root, cloneCommand, safeCommand)
        }
        historyMgr.endStep(history, StatusEnum.DONE, null)
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
            LOG.debug(String.format("Remove history on %s", branch))

            // Create new orphan branch and switch to it. The first commit made on this new branch
            // will have no parents and it will be the root of a new history totally disconnected from all the
            // other branches and commits
            var gitCommand = String.format("git checkout --orphan TEMP_BRANCH_%s", branch)
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

            // Stage All (new, modified, deleted) files. Equivalent to git add . (in Git Version 2.x)
            gitCommand = "git add -A"
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            try {
                // Create a new commit. Runs git add on any file that is 'tracked' and provide a message
                // for the commit
                gitCommand = String.format("git commit -am \"Reset history on %s\"", branch)
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            } catch (ex: RuntimeException) {
                // Ignored failed step
            }
            try {
                // Delete (with force) the passed in branch name
                gitCommand = String.format("git branch -D %s", branch)
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            } catch (ex: RuntimeException) {
                if (ex.message.equals("1", ignoreCase = true)) {
                    // Ignored failed step
                }
            }

            // move/rename a branch and the corresponding reflog
            // (i.e. rename the orphan branch - without history - to the passed in branch name)
            // Note : This fails with exit code 128 (git branch -m tmp_tag) when only folders in the subversion tag.
            // git commit -am above fails because no files
            gitCommand = String.format("git branch -m %s", branch)
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

            // i.e. if it is a branch
            if (!isTag) {

                // create the remote
                addRemote(workUnit, true)

                // push to remote
                gitCommand = String.format("git push -f origin %s", branch)
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE, String.format("Push %s with no history", branch))
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
