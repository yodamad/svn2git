package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.config.Constants
import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.*
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.repository.MappingRepository
import fr.yodamad.svn2git.service.client.GitlabAdmin
import fr.yodamad.svn2git.service.util.*
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GitLabApiException
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.Project
import org.gitlab4j.api.models.Visibility
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors

@Service
open class GitManager(val historyMgr: HistoryManager,
                      var mappingRepository: MappingRepository,
                      var applicationProperties: ApplicationProperties) {

    private val LOG = LoggerFactory.getLogger(GitManager::class.java)
    private val FAILED_TO_PUSH_BRANCH = "Failed to push branch"

    /**
     * Get freshinstance of GitlabAdmin object when called.
     *
     * @return GitlabAdmin
     */
    @Lookup
    open fun getGitlabAdminPrototype(): GitlabAdmin? { return null }

    /**
     * Create project in GitLab
     *
     * @param migration
     * @param workUnit
     * @throws GitLabApiException
     */
    @Throws(GitLabApiException::class)
    open fun createGitlabProject(migration: Migration) {
        val history: MigrationHistory = historyMgr.startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, migration.gitlabUrl + migration.gitlabGroup)
        var gitlabAdmin = getGitlabAdminPrototype()

        // If gitlabInfo.token is empty assure using values found in application.yml.
        // i.e. those in default GitlabAdmin object
        if (StringUtils.isEmpty(migration.gitlabToken)) {
            LOG.info("Already using default url and token")
        } else {
            // If gitlabInfo.token has a value we overide as appropriate
            if (!gitlabAdmin!!.api().gitLabServerUrl.equals(migration.gitlabUrl, ignoreCase = true) ||
                !gitlabAdmin.api().authToken.equals(migration.gitlabToken, ignoreCase = true)) {
                LOG.info("Overiding gitlab url and token")
                gitlabAdmin = GitlabAdmin(applicationProperties)
                val api = GitLabApi(migration.gitlabUrl, migration.gitlabToken)
                api.ignoreCertificateErrors = true
                gitlabAdmin.setGitlabApi(api)
            }
        }
        try {
            val group = gitlabAdmin!!.groupApi().getGroup(migration.gitlabGroup)

            // If no svn project specified, use svn group instead
            if (StringUtils.isEmpty(migration.svnProject)) {
                gitlabAdmin.projectApi().createProject(group.id, migration.svnGroup)
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } else {
                // split svn structure to create gitlab elements (group(s), project)
                val structure = migration.svnProject.split("/").toTypedArray()
                var groupId = group.id
                var currentPath = group.path
                if (structure.size > 2) {
                    for (module in 1 until structure.size - 1) {
                        val gitlabSubGroup = Group()
                        gitlabSubGroup.name = structure[module]
                        gitlabSubGroup.path = structure[module]
                        gitlabSubGroup.visibility = Visibility.INTERNAL
                        currentPath += String.format("/%s", structure[module])
                        gitlabSubGroup.parentId = groupId
                        try {
                            groupId = gitlabAdmin.groupApi().addGroup(gitlabSubGroup).id
                        } catch (gitlabApiEx: GitLabApiException) {
                            // Ignore error & get existing groupId
                            groupId = gitlabAdmin.groupApi().getGroup(currentPath).id
                            continue
                        }
                    }
                }
                val project = gitlabAdmin.groupApi().getProjects(groupId)
                    .stream()
                    .filter { p: Project -> p.name.equals(structure[structure.size - 1], ignoreCase = true) }
                    .findFirst()
                if (!project.isPresent) {
                    gitlabAdmin.projectApi().createProject(groupId, structure[structure.size - 1])
                    historyMgr.endStep(history, StatusEnum.DONE, null)
                } else {
                    throw GitLabApiException("Please remove the destination project '" + group.name + "/" + structure[structure.size - 1])
                }
            }
        } catch (exc: GitLabApiException) {
            val message: String? = exc.message?.replace(applicationProperties.gitlab.token, STARS)
            LOG.error("Gitlab errors are " + exc.validationErrors)
            historyMgr.endStep(history, StatusEnum.FAILED, message)
            throw exc
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
        if (!StringUtils.isEmpty(workUnit.migration.svnPassword)) {
            val escapedPassword = StringEscapeUtils.escapeJava(workUnit.migration.svnPassword)
            cloneCommand = initCommand(workUnit, workUnit.migration.svnUser, escapedPassword)
            safeCommand = initCommand(workUnit, workUnit.migration.svnUser, STARS)
        } else if (!StringUtils.isEmpty(applicationProperties.svn.password)) {
            val escapedPassword = StringEscapeUtils.escapeJava(applicationProperties.svn.password)
            cloneCommand = initCommand(workUnit, applicationProperties.svn.user, escapedPassword)
            safeCommand = initCommand(workUnit, applicationProperties.svn.user, STARS)
        } else {
            cloneCommand = initCommand(workUnit, null, null)
            safeCommand = cloneCommand
        }
        val history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_CHECKOUT,
            (if (workUnit.commandManager.isFirstAttemptMigration) "" else Constants.REEXECUTION_SKIPPING) + safeCommand)
        // Only Clone if first attempt at migration
        if (workUnit.commandManager.isFirstAttemptMigration) {
            Shell.execCommand(workUnit.commandManager, workUnit.root, cloneCommand, safeCommand)
        }
        historyMgr.endStep(history, StatusEnum.DONE, null)
    }

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
        val svnDirectoryDeleteList: List<String> = getSvnDirectoryDeleteList(workUnit.migration.id)
        // Initialise ignorePaths string that will be passed to git svn clone
        val ignorePaths: String = generateIgnorePaths(workUnit.migration.trunk, workUnit.migration.tags, workUnit.migration.branches, workUnit.migration.svnProject, svnDirectoryDeleteList)

        // regex with negative look forward allows us to choose the branch and tag names to keep
        val ignoreRefs: String = generateIgnoreRefs(workUnit.migration.branchesToMigrate, workUnit.migration.tagsToMigrate)
        val sCommand = String.format("%s git svn clone %s %s %s %s %s %s %s %s %s%s",  // Set password
            if (StringUtils.isEmpty(secret)) "" else if (Shell.isWindows) String.format("echo(%s|", secret) else String.format("echo %s |", secret),  // Set username
            if (StringUtils.isEmpty(username)) "" else String.format("--username %s", username),  // Set specific revision
            if (StringUtils.isEmpty(workUnit.migration.svnRevision)) "" else String.format("-r%s:HEAD", workUnit.migration.svnRevision),  // Set specific trunk
            if ((workUnit.migration.trunk == null || workUnit.migration.trunk != "trunk") && !workUnit.migration.flat) "" else buildTrunk(workUnit),  // Enable branches migrations
            if (workUnit.migration.branches == null) "" else String.format("--branches=%s/branches", workUnit.migration.svnProject),  // Enable tags migrations
            if (workUnit.migration.tags == null) "" else String.format("--tags=%s/tags", workUnit.migration.svnProject),  // Ignore some paths
            if (StringUtils.isEmpty(ignorePaths)) "" else ignorePaths,  // Ignore some ref
            if (StringUtils.isEmpty(ignoreRefs)) "" else ignoreRefs,  // Set flag for empty dir
            if (applicationProperties.getFlags().isGitSvnClonePreserveEmptyDirsOption) "--preserve-empty-dirs" else "",  // Set svn information
            if (workUnit.migration.svnUrl.endsWith("/")) workUnit.migration.svnUrl else String.format("%s/", workUnit.migration.svnUrl),
            workUnit.migration.svnGroup)

        // replace any multiple whitespaces and return
        return sCommand.replace("\\s{2,}".toRegex(), " ").trim { it <= ' ' }
    }

    /**
     * return list of svnDirectories to delete from a Set of Mappings
     *
     * @param migrationId
     * @return
     */
    open fun getSvnDirectoryDeleteList(migrationId: Long): List<String> {
        val mappings = mappingRepository.findByMigrationAndSvnDirectoryDelete(migrationId, true)
        val svnDirectoryDeleteList: MutableList<String> = ArrayList()
        val it: Iterator<Mapping> = mappings.iterator()
        while (it.hasNext()) {
            val mp = it.next()
            if (mp.isSvnDirectoryDelete) {
                svnDirectoryDeleteList.add(mp.svnDirectory)
            }
        }
        return svnDirectoryDeleteList
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
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                gitCommand = String.format("git commit -m \"Apply mappings on %s\"", branch)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                // git push
                gitCommand = String.format("%s --set-upstream origin %s", GIT_PUSH, branch.replace("origin/", ""))
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
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
        return results?.contains(StatusEnum.DONE_WITH_WARNINGS) ?: true
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
            if (applicationProperties.getFlags().isGitMvFOption) "-f" else "",
            if (applicationProperties.getFlags().isGitMvKOption()) "-k" else "",
            mapping.svnDirectory, mapping.gitDirectory, branch)

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
                        mv(workUnit, String.format("%s/%s", mapping.svnDirectory, d.fileName.toString()),
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
                        return@mapToInt Shell.execCommand(workUnit.commandManager, workUnit.directory,
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
            val historyCommand = String.format("git mv %s %s \"%s\" \"%s\" on %s", if (applicationProperties.getFlags().isGitMvFOption) "-f" else "", if (applicationProperties.getFlags().isGitMvKOption()) "-k" else "", svnDir, gitDir, branch)
            val gitCommand = String.format("git mv %s %s \"%s\" \"%s\"", if (applicationProperties.getFlags().isGitMvFOption) "-f" else "", if (applicationProperties.getFlags().isGitMvKOption()) "-k" else "", svnDir, gitDir)
            if (traceStep) history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, historyCommand)
            // git mv
            val exitCode = Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
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

    /**
     * Manage branches extracted from SVN
     *
     * @param workUnit
     * @param remotes
     */
    open fun manageBranches(workUnit: WorkUnit, remotes: List<String>) {
        listBranchesOnly(remotes, workUnit.migration.trunk)?.forEach(Consumer { b: String ->
            val warn: Boolean = pushBranch(workUnit, b)
            sleepBeforePush(workUnit, warn)
        })
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

    /**
     * Push a branch
     *
     * @param workUnit
     * @param branch   Branch to migrate
     */
    @Throws(RuntimeException::class)
    open fun pushBranch(workUnit: WorkUnit, branch: String): Boolean {
        var branchName = branch.replaceFirst("refs/remotes/origin/".toRegex(), "")
        branchName = branchName.replaceFirst("origin/".toRegex(), "")
        LOG.debug(String.format("Branch %s", branchName))
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, branchName)
        var gitCommand = String.format("git checkout -b %s %s", branchName, branch)
        try {
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
        } catch (iEx: IOException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
            historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
            return false
        } catch (iEx: InterruptedException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
            historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
            return false
        }
        if (workUnit.migration.svnHistory == "all") {
            try {
                addRemote(workUnit, true)
                gitCommand = String.format("%s --set-upstream origin %s", GIT_PUSH, branchName)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE, null)
            } catch (iEx: IOException) {
                LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
                return false
            } catch (iEx: InterruptedException) {
                LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
                return false
            }
        } else {
            removeHistory(workUnit, branchName, false, history)
        }
        return applyMapping(workUnit, branch)
    }

    /**
     * Manage tags extracted from SVN
     *
     * @param workUnit
     * @param remotes
     */
    open fun manageTags(workUnit: WorkUnit, remotes: List<String>) {
        listTagsOnly(remotes)?.forEach(Consumer { t: String ->
            val warn: Boolean = pushTag(workUnit, t)
            sleepBeforePush(workUnit, warn)
        })
    }

    /**
     * Push a tag
     *
     * @param workUnit Current work unit
     * @param tag      Tag to migrate
     */
    open fun pushTag(workUnit: WorkUnit, tag: String): Boolean {
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, tag)
        try {

            // derive local tagName from remote tag name
            val tagName = tag.replaceFirst(ORIGIN_TAGS.toRegex(), "")
            LOG.debug(String.format("Tag %s", tagName))

            // determine noHistory flag i.e was all selected or not
            val noHistory = workUnit.migration.svnHistory != "all"

            // checkout a new branch using local tagName and remote tag name
            var gitCommand = String.format("git checkout -b tmp_tag %s", tag)
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

            // If this tag does not contain any files we will ignore it and add warning to logs.
            if (!isFileInFolder(workUnit.directory)) {

                // Switch over to master
                gitCommand = "git checkout master"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // Now we can delete the branch tmp_tag
                gitCommand = "git branch -D tmp_tag"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.IGNORED, "Ignoring Tag: $tag : Because there are no files to commit.")
            } else {

                // creates a temporary orphan branch and renames it to tmp_tag
                if (noHistory) {
                    removeHistory(workUnit, "tmp_tag", true, history)
                }

                // Checkout master.
                gitCommand = "git checkout master"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // create tag from tmp_tag branch.
                gitCommand = String.format("git tag %s tmp_tag", tagName)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // add remote to master
                addRemote(workUnit, false)

                // push the tag to remote
                // crashes if branch with same name so prefixing with refs/tags/
                gitCommand = String.format("git push -u origin refs/tags/%s", tagName)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // delete the tmp_tag branch now that the tag has been created.
                gitCommand = "git branch -D tmp_tag"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE, null)
            }
        } catch (gitEx: IOException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx)
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        } catch (gitEx: InterruptedException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx)
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        }
        return false
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
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

            // Stage All (new, modified, deleted) files. Equivalent to git add . (in Git Version 2.x)
            gitCommand = "git add -A"
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            try {
                // Create a new commit. Runs git add on any file that is 'tracked' and provide a message
                // for the commit
                gitCommand = String.format("git commit -am \"Reset history on %s\"", branch)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
            } catch (ex: RuntimeException) {
                // Ignored failed step
            }
            try {
                // Delete (with force) the passed in branch name
                gitCommand = String.format("git branch -D %s", branch)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
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
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

            // i.e. if it is a branch
            if (!isTag) {

                // create the remote
                addRemote(workUnit, true)

                // push to remote
                gitCommand = String.format("git push -f origin %s", branch)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE, String.format("Push %s with no history", branch))
            }
        } catch (gitEx: IOException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx)
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        } catch (gitEx: InterruptedException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx)
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        }
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
        if (StringUtils.isEmpty(project)) {
            project = if (StringUtils.isEmpty(workUnit.migration.svnProject)) workUnit.migration.svnGroup else workUnit.migration.svnProject
        }
        val uri = URI.create(workUnit.migration.gitlabUrl)
        return String.format("git remote add origin %s://%s:%s@%s/%s/%s.git",
            uri.scheme,
            if (workUnit.migration.gitlabToken == null) applicationProperties.gitlab.account else workUnit.migration.user,
            if (safeMode) STARS else if (workUnit.migration.gitlabToken == null) applicationProperties.gitlab.token else workUnit.migration.gitlabToken,
            uri.authority,
            workUnit.migration.gitlabGroup,
            project)
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
                Shell.execCommand(workUnit.commandManager, workUnit.directory,
                    buildRemoteCommand(workUnit, null, false),
                    buildRemoteCommand(workUnit, null, true))
            } catch (rEx: IOException) {
                LOG.debug("Origin already added")
                // Skip
                // TODO : see to refactor, that's pretty ugly
            } catch (rEx: InterruptedException) {
                LOG.debug("Origin already added")
            } catch (rEx: RuntimeException) {
                LOG.debug("Origin already added")
            }
        }
    }
}
