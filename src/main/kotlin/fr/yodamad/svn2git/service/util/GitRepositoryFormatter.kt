package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.EMPTY
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.repository.MappingRepository
import fr.yodamad.svn2git.service.HistoryManager
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.io.File
import java.io.IOException
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
open class GitRepositoryFormatter(val historyMgr: HistoryManager,
                                  val applicationProperties: ApplicationProperties,
                                  val mappingRepository: MappingRepository) {

    private val LOG = LoggerFactory.getLogger(GitRepositoryFormatter::class.java)

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
            val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, "Push moved elements on $branch")
            try {
                // git commit
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand("add", target = "."))
                execCommand(workUnit.commandManager, workUnit.directory, commit("Apply mappings on $branch"))
                // git push
                val gitCommand = "$GIT_PUSH --set-upstream origin ${branch.replace("origin/", "")}"
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE)
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
        val history: MigrationHistory?
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

}
