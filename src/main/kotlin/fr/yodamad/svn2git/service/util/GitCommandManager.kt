package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.functions.*
import fr.yodamad.svn2git.service.MappingManager
import org.apache.commons.lang3.StringUtils.isEmpty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI

@Service
open class GitCommandManager(val mappingMgr: MappingManager,
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

        // Get list of svnDirectoryDelete
        val svnDirectoryDeleteList: List<String> = mappingMgr.getSvnDirectoryDeleteList(workUnit.migration.id)
        // Initialise ignorePaths string that will be passed to git svn clone
        val ignorePaths: String = generateIgnorePaths(workUnit.migration.trunk, workUnit.migration.tags, workUnit.migration.branches, workUnit.migration.svnProject, svnDirectoryDeleteList)

        // regex with negative look forward allows us to choose the branch and tag names to keep
        val ignoreRefs: String = generateIgnoreRefs(workUnit.migration.branchesToMigrate, workUnit.migration.tagsToMigrate)
        /* val sCommand = String.format("%s git svn clone %s %s %s %s %s %s %s %s %s%s",  // Set password
            if (isEmpty(secret)) EMPTY else if (isWindows) String.format("echo(%s|", secret) else String.format("echo %s |", secret),  // Set username
            if (isEmpty(username)) EMPTY else String.format("--username %s", username),  // Set specific revision
            if (isEmpty(workUnit.migration.svnRevision)) EMPTY else String.format("-r%s:HEAD", workUnit.migration.svnRevision),  // Set specific trunk
            if ((workUnit.migration.trunk == null || workUnit.migration.trunk != "trunk") && !workUnit.migration.flat) EMPTY else buildTrunk(workUnit),  // Enable branches migrations
            if (workUnit.migration.branches == null) EMPTY else String.format("--branches=%s/branches", workUnit.migration.svnProject),  // Enable tags migrations
            if (workUnit.migration.tags == null) EMPTY else String.format("--tags=%s/tags", workUnit.migration.svnProject),  // Ignore some paths
            if (isEmpty(ignorePaths)) EMPTY else ignorePaths,  // Ignore some ref
            if (isEmpty(ignoreRefs)) EMPTY else ignoreRefs,  // Set flag for empty dir
            if (applicationProperties.getFlags().isGitSvnClonePreserveEmptyDirsOption) "--preserve-empty-dirs" else EMPTY,  // Set svn information
            if (workUnit.migration.svnUrl.endsWith("/")) workUnit.migration.svnUrl else String.format("%s/", workUnit.migration.svnUrl),
            workUnit.migration.svnGroup)
        */

        val cloneCommand = String.format("%s git svn clone %s %s %s %s %s %s %s %s %s%s",
            formattedOrEmpty(secret, "echo %s |", "echo(%s|"),
            formattedOrEmpty(username, "--username %s"),
            formattedOrEmpty(workUnit.migration.svnRevision, "-r%s:HEAD"),
            setTrunk(workUnit),
            setSvnElement("branches", workUnit.migration.branches, workUnit),
            setSvnElement("tags", workUnit.migration.tags, workUnit),
            ignorePaths, ignoreRefs,
            if (applicationProperties.getFlags().isGitSvnClonePreserveEmptyDirsOption) "--preserve-empty-dirs" else EMPTY,
            if (workUnit.migration.svnUrl.endsWith("/")) workUnit.migration.svnUrl else "${workUnit.migration.svnUrl}/",
            workUnit.migration.svnGroup)

        // replace any multiple whitespaces and return
        return cloneCommand.replace("\\s{2,}".toRegex(), " ").trim { it <= ' ' }
    }

    private fun setTrunk(workUnit: WorkUnit) =
        if ((workUnit.migration.trunk == null || workUnit.migration.trunk != "trunk") && !workUnit.migration.flat) EMPTY
        else buildTrunk(workUnit)

    private fun setSvnElement(elementName: String, element: String?, workUnit: WorkUnit) =
        if (element == null) EMPTY else "--$elementName=${workUnit.migration.svnProject}/$elementName"

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
