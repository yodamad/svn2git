package fr.yodamad.svn2git.service.util

import com.github.jknack.handlebars.Handlebars
import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.*
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.io.Shell.isWindows
import fr.yodamad.svn2git.service.HistoryManager
import fr.yodamad.svn2git.service.MappingManager
import org.apache.commons.lang3.StringUtils.isEmpty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.io.StringWriter
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.*

@Service
open class GitCommandManager(val historyMgr: HistoryManager,
                        val mappingMgr: MappingManager,
                        var applicationProperties: ApplicationProperties) {

    private val LOG = LoggerFactory.getLogger(GitCommandManager::class.java)

    /**
     * Init command with or without password in clear
     *
     * @param workUnit Current workUnit
     * @param username Username to use
     * @param secret   Escaped password
     * @return
     */
    open fun initCommand(workUnit: WorkUnit, username: String?, secret: String?): String {
        val cloneCommand = String.format("git svn clone %s %s %s",
            formattedOrEmpty(username, "--username %s"),
            initOptions(workUnit),
            buildSvnCompleteUrl(workUnit))

        // replace any multiple whitespaces and return
        return cloneCommand.replace("\\s{2,}".toRegex(), " ").trim { it <= ' ' }
    }

    open fun initOptions(workUnit: WorkUnit) : String {
        val svnDirectoryDeleteList: List<String> = mappingMgr.getSvnDirectoryDeleteList(workUnit.migration.id)
        return String.format("%s %s %s %s %s %s",
            formattedOrEmpty(workUnit.migration.svnRevision, "-r%s:HEAD"),
            setTrunk(workUnit),
            setSvnElement("branches", workUnit.migration.branches, workUnit),
            setSvnElement("tags", workUnit.migration.tags, workUnit),
            generateIgnorePaths(workUnit.migration.trunk, workUnit.migration.tags, workUnit.migration.branches, workUnit.migration.svnProject, svnDirectoryDeleteList),
            if (workUnit.migration.emptyDirs) "--preserve-empty-dirs"
            else if (workUnit.migration.emptyDirs == null && applicationProperties.getFlags().isGitSvnClonePreserveEmptyDirsOption) "--preserve-empty-dirs" else EMPTY,
        )
    }

    open fun generateGitSvnCloneScript(workUnit: WorkUnit, gitSvnCloneCommand: String): String {

        val scriptInfo = ScriptInfo(gitSvnCloneCommand, workUnit.migration.svnUser, workUnit.migration.svnPassword.escape(), "${workUnit.directory}")

        val handlebars = Handlebars()
        val template = handlebars.compile("templates/scripts/git-svn-clone.sh")

        val fileToWrite = File("${workUnit.directory}/git-svn-clone.sh")
        val writer = StringWriter()
        template.apply(scriptInfo, writer)
        fileToWrite.writeText(writer.toString())

        if (!isWindows) {
            Files.setPosixFilePermissions(
                fileToWrite.toPath(),
                setOf(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE, GROUP_READ, OTHERS_READ)
            )
        }
        return fileToWrite.path
    }

    open fun generateGitSvnClonePackageForWindows(workUnit: WorkUnit, cloneOptions: String?) {

        val scriptInfo = ScriptInfo("", workUnit.migration.svnUser, workUnit.migration.svnPassword,
            "${workUnit.directory}", buildSvnCompleteUrl(workUnit), cloneOptions)

        val handlebars = Handlebars()
        var template = handlebars.compile("templates/scripts/win/git-command.ps1")

        var fileToWrite = File("${workUnit.directory}/git-command.ps1")
        var writer = StringWriter()
        template.apply(scriptInfo, writer)
        fileToWrite.writeText(writer.toString())

        template = handlebars.compile("templates/scripts/win/git-svn-clone.ps1")
        fileToWrite = File("${workUnit.directory}/git-svn-clone.ps1")
        writer = StringWriter()
        template.apply(null, writer)
        fileToWrite.writeText(writer.toString())

    }

    /**
     * Set trunk information
     */
    private fun setTrunk(workUnit: WorkUnit) =
        if ((workUnit.migration.trunk == null || workUnit.migration.trunk != "trunk") && !workUnit.migration.flat) EMPTY
        else buildTrunk(workUnit)

    /**
     * Set element (branch or tag)
     */
    private fun setSvnElement(elementName: String, element: String?, workUnit: WorkUnit): String {
        val elt = if (workUnit.migration.uppercase) elementName.uppercase() else elementName
        return if (element == null) EMPTY
            else "--$elementName=${workUnit.migration.svnProject}/$elt"
    }

    /**
     * Build command to add remote
     *
     * @param workUnit Current work unit
     * @param project  Current project
     * @param safeMode safe mode for logs
     * @return
     */
    open fun buildRemoteCommand(workUnit: WorkUnit, project: String?, safeMode: Boolean): String? {
        var project = project
        if (isEmpty(project)) {
            project = when {
                isEmpty(workUnit.migration.svnProject) && isEmpty(workUnit.migration.gitlabProject) -> workUnit.migration.svnGroup
                isEmpty(workUnit.migration.gitlabProject) -> workUnit.migration.svnProject
                else -> workUnit.migration.gitlabProject
            }
        }
        val uri = URI.create(workUnit.migration.gitlabUrl)
        return "git remote add origin ${uri.scheme}://${getAccount(workUnit)}:${safeString(workUnit, safeMode)}@${uri.authority}/${workUnit.migration.gitlabGroup}/${project}.git"
    }

    open fun sleepBeforePush(workUnit: WorkUnit, warn: Boolean) {
        workUnit.warnings.set(workUnit.warnings.get() || warn)
        if (applicationProperties.gitlab.gitPushPauseMilliSeconds > 0) {
            try {
                LOG.info(String.format("Waiting gitPushPauseMilliSeconds:%s", applicationProperties.gitlab.gitPushPauseMilliSeconds))
                Thread.sleep(applicationProperties.gitlab.gitPushPauseMilliSeconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw RuntimeException(e)
            }
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

    private fun getAccount(workUnit: WorkUnit) =
        if (workUnit.migration.gitlabToken == null) applicationProperties.gitlab.account
        else workUnit.migration.user

    private fun safeString(workUnit: WorkUnit, safeMode: Boolean) =
        when {
            safeMode -> STARS
            workUnit.migration.gitlabToken == null -> applicationProperties.gitlab.token
            else -> workUnit.migration.gitlabToken
        }
}

/**
 * Info to inject in generated script
 */
data class ScriptInfo(val svnCommand: String, val svnUser: String, val svnPassword: String, val workingDir: String, val svnUrl: String? = "", val cloneOptions: String? = "")
