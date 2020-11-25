package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.*
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.repository.MappingRepository
import fr.yodamad.svn2git.service.util.*
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isEmpty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.io.File
import java.io.IOException
import java.nio.charset.Charset.defaultCharset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

@Service
open class GitManager(val historyMgr: HistoryManager,
                      val gitCommandManager: GitCommandManager,
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
     * Git clean
     */
    open fun gitClean(commandManager: CommandManager, workUnit: WorkUnit) {
        try {
            val gitCommand = "git reflog expire --expire=now --all"
            execCommand(commandManager, workUnit.directory, gitCommand)
        } catch (rEx: RuntimeException) {
            LOG.error("Failed to run git reflog expire --expire=now --all")
        }
        try {
            val gitCommand = "git gc --prune=now --aggressive"
            execCommand(commandManager, workUnit.directory, gitCommand)
        } catch (rEx: RuntimeException) {
            LOG.error("Failed to run git gc --prune=now --aggressive")
        }
        var gitCommand = "git reset HEAD"
        execCommand(commandManager, workUnit.directory, gitCommand)
        gitCommand = "git clean -fd"
        execCommand(commandManager, workUnit.directory, gitCommand)
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
     * Apply mappings configured
     *
     * @param workUnit
     * @param branch   Branch to process
     */
    open fun applyMapping(workUnit: WorkUnit, branch: String): Boolean {
        // Get only the mappings (i.e. where svnDirectoryDelete is false)
        val mappings = mappingRepository.findByMigrationAndSvnDirectoryDelete(workUnit.migration.id, false)
        var workDone = false
        var results: MutableList<StatusEnum?>? = null
        if (!CollectionUtils.isEmpty(mappings)) {
            // Extract mappings with regex
            val regexMappings = mappings.stream()
                .filter { mapping: Mapping? -> !StringUtils.isEmpty(mapping!!.regex) && "*" != mapping.regex }
                .collect(Collectors.toList())
            results = regexMappings.stream()
                .map { mapping: Mapping? -> mvRegex(workUnit, mapping!!, branch) }
                .collect(Collectors.toList())

            // Remove regex mappings
            mappings.removeAll(regexMappings)
            results.addAll(
                mappings.stream()
                    .map { mapping: Mapping? -> mvDirectory(workUnit, mapping!!, branch) }
                    .collect(Collectors.toList()))
            workDone = results.contains(StatusEnum.DONE)
        }
        if (workDone) {
            val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, String.format("Push moved elements on %s", branch))
            try {
                // git commit
                var gitCommand = "git add ."
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                gitCommand = String.format("git commit -m \"Apply mappings on %s\"", branch)
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                // git push
                gitCommand = String.format("%s --set-upstream origin %s", GIT_PUSH, branch.replace("origin/", ""))
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } catch (iEx: IOException) {
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
                return false
            } catch (iEx: InterruptedException) {
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
                return false
            }
        }

        // No mappings, OK
        return results?.contains(StatusEnum.DONE_WITH_WARNINGS) ?: false
        // Some errors, WARNING to be set
    }

    /**
     * Apply git mv
     *
     * @param workUnit
     * @param mapping  Mapping to apply
     * @param branch   Current branch
     */
    open fun mvDirectory(workUnit: WorkUnit, mapping: Mapping, branch: String): StatusEnum? {
        var history: MigrationHistory?
        val msg = String.format("git mv %s %s \"%s\" \"%s\" on %s",
            fOptionOrEmpty(), kOptionOrEmpty(), mapping.svnDirectory, mapping.gitDirectory, branch)

        // If git directory in mapping contains /, we need to create root directories must be manually created
        if (mapping.gitDirectory.contains("/") && mapping.gitDirectory != "/") {
            val tmpPath = AtomicReference(Paths.get(workUnit.directory))
            Arrays.stream(mapping.gitDirectory.split("/").toTypedArray())
                .forEach { dir: String? ->
                    val newPath = Paths.get(tmpPath.toString(), dir)
                    if (!Files.exists(newPath)) {
                        try {
                            Files.createDirectory(newPath)
                        } catch (ioEx: IOException) {
                            ioEx.printStackTrace()
                        }
                    }
                    tmpPath.set(newPath)
                }
        }
        try {
            val files = Files.list(Paths.get(workUnit.directory, mapping.svnDirectory))
            return if (mapping.gitDirectory == "/" || mapping.gitDirectory == "." || mapping.gitDirectory.contains("/")) {
                history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg)
                var result = StatusEnum.DONE
                val useGitDir = mapping.gitDirectory.contains("/")
                // For root directory, we need to loop for subdirectory
                val results: List<StatusEnum> = files
                    .map { d: Path ->
                        mv(workUnit, "${mapping.svnDirectory}/${d.fileName.toString()}",
                            if (useGitDir) mapping.gitDirectory else d.fileName.toString(),
                            branch, false)
                    }
                    .collect(Collectors.toList()) as List<StatusEnum>
                if (results.isEmpty()) {
                    result = StatusEnum.IGNORED
                }
                if (results.contains(StatusEnum.DONE_WITH_WARNINGS)) {
                    result = StatusEnum.DONE_WITH_WARNINGS
                }
                historyMgr.endStep(history, result, null)
                result
            } else {
                mv(workUnit, mapping.svnDirectory, mapping.gitDirectory, branch, true)
            }
        } catch (gitEx: IOException) {
            history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg)
            if (gitEx is NoSuchFileException) {
                historyMgr.endStep(history, StatusEnum.IGNORED, null)
            } else {
                historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
            }
            return StatusEnum.IGNORED
        }
    }

    /**
     * Apply git mv
     *
     * @param workUnit Current work unit
     * @param mapping  Mapping to apply
     * @param branch   Current branch
     */
    open fun mvRegex(workUnit: WorkUnit, mapping: Mapping, branch: String) : StatusEnum {
        val msg = String.format("git mv %s %s \"%s\" \"%s\" based on regex %s on %s",
            if (applicationProperties.getFlags().isGitMvFOption()) "-f" else "",
            if (applicationProperties.getFlags().isGitMvKOption()) "-k" else "",
            mapping.svnDirectory, mapping.gitDirectory, mapping.regex, branch)

        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg)
        var regex = mapping.regex
        if (mapping.regex.startsWith("*")) {
            regex = '.'.toString() + mapping.regex
        }
        val p = Pattern.compile(regex)
        val fullPath = Paths.get(workUnit.directory, mapping.svnDirectory)
        try {
            val walker = Files.walk(fullPath)
            val result = walker
                .map { obj: Path -> obj.toString() }
                .filter { s: String -> s != fullPath.toString() }
                .map { s: String -> s.substring(workUnit.directory.length) }
                .map { input: String? -> p.matcher(input) }
                .filter { obj: Matcher -> obj.find() }
                .map { matcher: Matcher -> matcher.group(0) }
                .mapToInt { el: String? ->
                    try {
                        val gitPath: Path = if (File(el).parentFile == null) {
                            Paths.get(workUnit.directory, mapping.gitDirectory)
                        } else {
                            Paths.get(workUnit.directory, mapping.gitDirectory, File(el).parent)
                        }
                        if (!Files.exists(gitPath)) {
                            Files.createDirectories(gitPath)
                        }
                        return@mapToInt execCommand(workUnit.commandManager, workUnit.directory,
                            String.format("git mv %s %s \"%s\" \"%s\"",
                                if (applicationProperties.getFlags().isGitMvFOption()) "-f" else "",
                                if (applicationProperties.getFlags().isGitMvKOption()) "-k" else "",
                                el, Paths.get(mapping.gitDirectory, el).toString()))
                    } catch (e: InterruptedException) {
                        return@mapToInt ERROR_CODE
                    } catch (e: IOException) {
                        return@mapToInt ERROR_CODE
                    }
                }.sum()

            return if (result > 0) {
                historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, null)
                StatusEnum.DONE_WITH_WARNINGS
            } else {
                historyMgr.endStep(history, StatusEnum.DONE, null)
                StatusEnum.DONE
            }
        } catch (ioEx: IOException) {
            historyMgr.endStep(history, StatusEnum.FAILED, ioEx.message)
            return StatusEnum.DONE_WITH_WARNINGS
        }
    }

    /**
     * Apply git mv
     *
     * @param workUnit
     * @param svnDir   Origin SVN element
     * @param gitDir   Target Git element
     * @param branch   Current branch
     */
    open fun mv(workUnit: WorkUnit, svnDir: String, gitDir: String, branch: String, traceStep: Boolean): StatusEnum? {
        try {
            val gitMvPauseMilliSeconds: Long = applicationProperties.getGitlab().getGitMvPauseMilliSeconds()
            if (gitMvPauseMilliSeconds > 0) {
                LOG.info(String.format("Waiting %d MilliSeconds between git mv operations", gitMvPauseMilliSeconds))
                Thread.sleep(gitMvPauseMilliSeconds)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException(e)
        }
        var history: MigrationHistory? = null
        return try {
            val historyCommand = "git mv ${fOptionOrEmpty()} ${kOptionOrEmpty()} \"$svnDir\" \"$gitDir\" on $branch"
            val gitCommand = "git mv ${fOptionOrEmpty()} ${kOptionOrEmpty()} \"$svnDir\" \"$gitDir\""

            if (traceStep) history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, historyCommand)
            // git mv
            val exitCode = execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            if (ERROR_CODE == exitCode) {
                if (traceStep) historyMgr.endStep(history, StatusEnum.IGNORED, null)
                StatusEnum.IGNORED
            } else {
                if (traceStep) historyMgr.endStep(history, StatusEnum.DONE, null)
                StatusEnum.DONE
            }
        } catch (gitEx: IOException) {
            LOG.error("Failed to mv directory", gitEx)
            if (traceStep) historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
            StatusEnum.DONE_WITH_WARNINGS
        } catch (gitEx: InterruptedException) {
            LOG.error("Failed to mv directory", gitEx)
            if (traceStep) historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
            StatusEnum.DONE_WITH_WARNINGS
        }
    }

    private fun fOptionOrEmpty() = optionOrEmpty(applicationProperties.getFlags().isGitMvFOption, "-f")
    private fun kOptionOrEmpty() = optionOrEmpty(applicationProperties.getFlags().isGitMvKOption, "-k")
    private fun optionOrEmpty(option: Boolean, flag: String) = if (option) flag else EMPTY

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
