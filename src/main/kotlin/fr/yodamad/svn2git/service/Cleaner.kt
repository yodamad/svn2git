package fr.yodamad.svn2git.service

import com.madgag.git.bfg.cli.Main
import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.data.CleanedFiles
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.MigrationRemovedFile
import fr.yodamad.svn2git.domain.enumeration.Reason
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StatusEnum.DONE
import fr.yodamad.svn2git.domain.enumeration.StatusEnum.DONE_WITH_WARNINGS
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum.*
import fr.yodamad.svn2git.domain.enumeration.SvnLayout
import fr.yodamad.svn2git.domain.enumeration.SvnLayout.TAG
import fr.yodamad.svn2git.functions.*
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.repository.MigrationRemovedFileRepository
import fr.yodamad.svn2git.service.client.ArtifactoryAdmin
import fr.yodamad.svn2git.service.util.checkout
import fr.yodamad.svn2git.service.util.deleteBranch
import fr.yodamad.svn2git.service.util.gc
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.data.util.Pair
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.lang.Long.valueOf
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.stream.Collectors

/**
 * Cleaning operations
 */
@Service
open class Cleaner(val historyMgr: HistoryManager,
                   val mrfRepo: MigrationRemovedFileRepository,
                   val applicationProperties: ApplicationProperties,
                   private val artifactoryAdmin: ArtifactoryAdmin) {

    private val LOG = LoggerFactory.getLogger(Cleaner::class.java)
    private val GIT_LIST = "git-list"
    private val SVN_LIST = "svn-list"
    private val TAGS = "tags/"

    /**
     * List files that are going to be cleaned by BFG
     *
     * @param workUnit Current migration information
     * @throws IOException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun listCleanedFiles(workUnit: WorkUnit): CleanedFilesManager? {
        val cleanedFilesMap: MutableMap<String, CleanedFiles> = LinkedHashMap()
        val history: MigrationHistory = historyMgr.startStep(workUnit.migration, StepEnum.LIST_REMOVED_FILES, "")
        val warnings = AtomicBoolean(false)
        if (!StringUtils.isEmpty(workUnit.migration.trunk)) {
            val cleanedFilesTrunk: CleanedFiles = listCleanedFilesInSvnLocation(workUnit, workUnit.migration.trunk, SvnLayout.TRUNK)
            cleanedFilesMap[workUnit.migration.trunk] = cleanedFilesTrunk
        }
        val remotes = listRemotes(workUnit.directory)
        listBranchesOnly(remotes, workUnit.migration.trunk)!!.forEach(
            Consumer { b: String ->
                try {
                    // get branchName
                    var branchName = b.replaceFirst("refs/remotes/origin/".toRegex(), "")
                    branchName = branchName.replaceFirst("origin/".toRegex(), "")
                    LOG.debug("Branch $branchName", branchName)
                    // checkout new branchName from existing remote branch
                    val gitCommand = String.format("git checkout -b \"%s\" \"%s\"", branchName, b)
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                    // listCleanedFilesInSvnLocation
                    val cleanedFilesBranch: CleanedFiles = listCleanedFilesInSvnLocation(workUnit, b.replace("origin", "branches"), SvnLayout.BRANCH)
                    cleanedFilesMap[b.replace("origin", "branches")] = cleanedFilesBranch
                    // back to master
                    execCommand(workUnit.commandManager, workUnit.directory, checkout())
                    // delete the temporary branch
                    execCommand(workUnit.commandManager, workUnit.directory, deleteBranch(branchName))
                } catch (ioEx: IOException) {
                    LOG.warn("Failed to list removed files on $b")
                    warnings.set(true)
                } catch (ioEx: InterruptedException) {
                    LOG.warn("Failed to list removed files on $b")
                    warnings.set(true)
                }
            }
        )
        listTagsOnly(remotes)!!.forEach(
            Consumer { t: String ->
                try {
                    // checkout new branch 'tmp_tag' from existing tag
                    val gitCommand = String.format("git checkout -b tmp_tag \"%s\"", t)
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                    // listCleanedFilesInSvnLocation
                    val cleanedFilesTag: CleanedFiles = listCleanedFilesInSvnLocation(workUnit, t.replace("origin", "tags"), TAG)
                    cleanedFilesMap[t.replace("origin", "tags")] = cleanedFilesTag
                    // back to master
                    execCommand(workUnit.commandManager, workUnit.directory, checkout())
                    // delete the temporary branch
                    execCommand(workUnit.commandManager, workUnit.directory, deleteBranch("tmp_tag"))
                } catch (ioEx: IOException) {
                    LOG.warn(String.format("Failed to list removed files on %s", t))
                    warnings.set(true)
                } catch (ioEx: InterruptedException) {
                    LOG.warn(String.format("Failed to list removed files on %s", t))
                    warnings.set(true)
                }
            }
        )

        // back to master
        execCommand(workUnit.commandManager, workUnit.directory, checkout())

        // get list of files that will in principle be removed
        val migrationRemovedFiles: List<MigrationRemovedFile> = mrfRepo.findAllByMigration_Id(workUnit.migration.id)
        val sMigrationRemovedFiles = migrationRemovedFiles.stream()
            .map { element: MigrationRemovedFile -> "($element.svnLocation)/$element.path" }
            .collect(Collectors.joining(", "))
        if (warnings.get()) {
            historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, sMigrationRemovedFiles)
        } else {
            historyMgr.endStep(history, DONE, sMigrationRemovedFiles)
        }
        return CleanedFilesManager(cleanedFilesMap)
    }

    /**
     * List files that are going to be cleaned by BFG
     *
     * @param workUnit
     * @param svnLocation
     * @param svnLayout
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun listCleanedFilesInSvnLocation(workUnit: WorkUnit, svnLocation: String, svnLayout: SvnLayout): CleanedFiles {
        val cleanedFiles = CleanedFiles(svnLocation, svnLayout)
        val workingPath = Paths.get(workUnit.directory)
        inspectPath(workUnit, workingPath, svnLocation, cleanedFiles)
        return cleanedFiles
    }

    /**
     * Inspect current path (recursively)
     *
     * @param workUnit    Current migration information
     * @param workingPath Current path
     * @param svnLocation Current svn location (trunk, branch, ...)
     * @throws IOException
     */
    @Throws(IOException::class)
    open fun inspectPath(workUnit: WorkUnit, workingPath: Path, svnLocation: String, cleanedFiles: CleanedFiles) {
        val initialPath = Paths.get(workUnit.directory)
        val pathFilter = DirectoryStream.Filter { path: Path ->
            if (path.toFile().isDirectory) {
                // Ignore git folder
                if (path.toFile().name == ".git") {
                    return@Filter false
                }
                inspectPath(workUnit, path, svnLocation, cleanedFiles)
                return@Filter false
            } else {
                val file = path.toFile()
                val isForbiddenExtension: Boolean = isForbiddenExtension(workUnit, path)
                val exceedsMaxSize: Boolean = exceedsMaxSize(workUnit, path)
                val isToBeDeleted = (file.isFile
                    && (isForbiddenExtension || exceedsMaxSize))
                if (file.isFile) {
                    cleanedFiles.fileCountBeforeClean++
                    cleanedFiles.fileSizeTotalBeforeClean += file.length()
                    if (isToBeDeleted) {
                        cleanedFiles.deletedFileCountAfterClean++
                    } else {
                        cleanedFiles.fileCountAfterClean++
                        cleanedFiles.fileSizeTotalAfterClean += file.length()
                    }
                }
                return@Filter isToBeDeleted
            }
        }

        Files.newDirectoryStream(workingPath, pathFilter).use { dirStream ->
            for (p in dirStream) {
                val reason = if (isForbiddenExtension(workUnit, p)) Reason.EXTENSION else Reason.SIZE
                val fileSize: Long = getFileSize(p)
                val mrf = MigrationRemovedFile()
                    .migration(workUnit.migration)
                    .svnLocation(svnLocation)
                    .path(initialPath.relativize(p).toString())
                    .reason(reason)
                    .fileSize(fileSize)
                this.mrfRepo.save(mrf)
            }
        }

        // Upload files from tags
        if (svnLocation.startsWith(TAGS)) {
            // Upload to Gitlab
            if (applicationProperties.gitlab.uploadToRegistry && workUnit.migration.uploadType == "gitlab") {
                val history = historyMgr.startStep(workUnit.migration, UPLOAD_TO_GITLAB, svnLocation)
                var globalStatus = true
                Files.newDirectoryStream(workingPath, pathFilter).use { dirStream ->
                    for (p in dirStream) {
                        val status = uploadFileToGitlab(
                            workUnit.migration.gitlabUrl,
                            if (workUnit.migration.gitlabToken != null) workUnit.migration.gitlabToken else applicationProperties.gitlab.token,
                            workUnit.migration.gitlabProjectId,
                            if (workUnit.migration.gitlabProject.isEmpty()) workUnit.migration.svnGroup else workUnit.migration.gitlabProject.split(
                                "/"
                            ).last(),
                            extractVersion(svnLocation),
                            p.fileName.toString(),
                            p.toString()
                        )
                        println("Upload of $p is $status")
                        globalStatus = globalStatus && (status == 201)
                    }
                }
                historyMgr.endStep(history, if (globalStatus) DONE else DONE_WITH_WARNINGS)
            }
            // Upload to Artifactory
            if (applicationProperties.artifactory.enabled && workUnit.migration.uploadType == "artifactory") {
                val history = historyMgr.startStep(workUnit.migration, UPLOAD_TO_ARTIFACTORY, svnLocation)
                Files.newDirectoryStream(workingPath, pathFilter).use { dirStream ->
                    for (p in dirStream) {
                        artifactoryAdmin.uploadArtifact(
                            File(p.toString()),
                            workUnit.migration.gitlabGroup,
                            if (workUnit.migration.gitlabProject.isEmpty()) workUnit.migration.svnGroup else workUnit.migration.gitlabProject,
                            extractVersion(svnLocation))
                    }
                }
                historyMgr.endStep(history, DONE)
            }

            // Upload to Nexus
            if (applicationProperties.nexus.enabled && workUnit.migration.uploadType == "nexus") {
                val history = historyMgr.startStep(workUnit.migration, UPLOAD_TO_NEXUS, svnLocation)
                var globalStatus = true
                Files.newDirectoryStream(workingPath, pathFilter).use { dirStream ->
                    for (p in dirStream) {
                        val status = uploadFileToNexus(
                            applicationProperties.nexus.url,
                            applicationProperties.nexus.repository,
                            applicationProperties.nexus.user,
                            applicationProperties.nexus.password,
                            workUnit.migration.gitlabGroup,
                            if (workUnit.migration.gitlabProject.isEmpty()) workUnit.migration.svnGroup else workUnit.migration.gitlabProject,
                            extractVersion(svnLocation),
                            p.fileName.toString(),
                            p.toString()
                        )
                        println("Upload of $p is $status")
                        globalStatus = globalStatus && (status == 201)
                    }
                }
                historyMgr.endStep(history, if (globalStatus) DONE else DONE_WITH_WARNINGS)
            }
        }
    }

    /**
     * Get File Size of the file found at path.
     *
     * @param path The path of a file
     * @return
     */
    private fun getFileSize(path: Path): Long {
        val file = path.toFile()
        return if (!file.exists() || !file.isFile) {
            -1L
        } else {
            valueOf(file.length())
        }
    }

    /**
     * Clean files with forbiddene extensions if configured
     *
     * @param workUnit
     * @return
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun cleanForbiddenExtensions(workUnit: WorkUnit): Boolean {
        var clean = false
        if (!StringUtils.isEmpty(workUnit.migration.forbiddenFileExtensions)) {
            // needed?
            execCommand(workUnit.commandManager, workUnit.directory, gc())

            // 3.1 Clean files based on their extensions
            Arrays.stream(workUnit.migration.forbiddenFileExtensions.split(",").toTypedArray())
                .forEach { s: String ->
                    val innerHistory = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CLEANING,
                        "Remove files with extension : ${s.toLowerCase()} and ${s.toUpperCase()}")
                    try {
                        Main.main(arrayOf("--delete-files", s.toLowerCase(), "--no-blob-protection", workUnit.directory))
                        Main.main(arrayOf("--delete-files", s.toUpperCase(), "--no-blob-protection", workUnit.directory))
                        historyMgr.endStep(innerHistory, DONE, null)
                    } catch (exc: Throwable) {
                        historyMgr.endStep(innerHistory, StatusEnum.FAILED, exc.message)
                        workUnit.warnings.set(true)
                    }
                }
            clean = true
        }
        return clean
    }

    /**
     * Clean large files if configured
     *
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun cleanLargeFiles(workUnit: WorkUnit): Boolean {
        var clean = false
        if (!StringUtils.isEmpty(workUnit.migration.maxFileSize) && Character.isDigit(workUnit.migration.maxFileSize[0])) {
            // 3.2 Clean files based on size
            val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CLEANING,
                "Remove files bigger than ${workUnit.migration.maxFileSize}")

            // This is necessary for BFG to work
            execCommand(workUnit.commandManager, workUnit.directory, gc())
            Main.main(arrayOf(
                "--strip-blobs-bigger-than", workUnit.migration.maxFileSize,
                "--no-blob-protection", workUnit.directory))
            clean = true
            historyMgr.endStep(history, DONE)
        }
        return clean
    }

    /**
     * Used to delete folder that is pushed to artifactory.
     * Note : This is only useful if the foldername is unique. Apparently not possible to provide a folder path.
     * Note : Needed to put this before other cleaning options (extension and size)
     *
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun cleanFolderWithBFG(workUnit: WorkUnit): Boolean {
        var clean = false
        if (applicationProperties.artifactory.enabled && StringUtils.isNotEmpty(applicationProperties.artifactory.deleteFolderWithBFG)) {
            // Delete folders based on a folder name
            val history = historyMgr.startStep(workUnit.migration, StepEnum.ARTIFACTORY_FOLDER_CLEANING,
                "Remove Folders called ${applicationProperties.artifactory.deleteFolderWithBFG}")

            execCommand(workUnit.commandManager, workUnit.directory, gc())
            Main.main(arrayOf(
                "--delete-folders", "{" + applicationProperties.artifactory.deleteFolderWithBFG + "}",
                "--no-blob-protection", workUnit.directory))
            clean = true
            historyMgr.endStep(history, DONE)
        }
        return clean
    }

    /**
     * Clean elements removed in svn but clone in git svn
     * @param tags Flag to know if tags or branches target
     */
    open fun cleanElementsOn(workUnit: WorkUnit, tags: Boolean) {

        val elements = if (tags) Pair(workUnit.migration.tags, "tags")
                        else Pair(workUnit.migration.branches, "branches")

        if (elements.first != null) {
            val history = historyMgr.startStep(workUnit.migration, StepEnum.BRANCH_CLEAN, "Clean removed SVN ${elements.second}")
            val pairInfo = cleanRemovedElements(workUnit, tags)
            if (pairInfo!!.first.get()) {
                //  Some branches have failed
                historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, "Failed to remove ${elements.second} %s ${pairInfo.second}")
            } else {
                historyMgr.endStep(history, DONE)
            }
        }
    }

    /**
     * Clean elements removed in svn but clone in git svn
     *
     * @param workUnit Current work unit
     * @param isTags   Flag to know if it's tags processing
     * @return Pair containing warning flags & potential failed branches
     * @throws IOException
     * @throws InterruptedException
     */
    @Throws(IOException::class, InterruptedException::class)
    open fun cleanRemovedElements(workUnit: WorkUnit, isTags: Boolean): Pair<AtomicBoolean, List<String>>? {
        val withWarning = AtomicBoolean()
        val failedBranches: MutableList<String> = ArrayList()

        // ######### List git branch ##################################

        // The execution of "git branch -r" will give us a list of the branches or tags that were really physically "clone"
        //     In the command "git svn clone"
        // This should normally correspond to the requested branches and tags (through use of branch or tag filter)
        //    However the ignore-refs configuration fails for certain project(s) - non identified issue.
        // Also if no branch or tag filter was indicated you may have 'deleted' branches or tags.
        val gitBranchList = String.format("git branch -r > %s", GIT_LIST)
        execCommand(workUnit.commandManager, workUnit.directory, gitBranchList)
        val gitElementsToDelete: MutableList<String> = if (isTags) {
            Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map { l: String -> l.trim { it <= ' ' }.replace("origin/", "").decode().encode() }
                .filter { t: String -> t.startsWith("tags") }
                .map { l: String -> l.replace(TAGS, "") }
                .filter { l: String -> !l.equals(workUnit.migration.trunk, ignoreCase = true) }
                .collect(Collectors.toList())
        } else {
            Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map { l: String -> l.trim { it <= ' ' }.replace("origin/", "").decode().encode() }
                .filter { l: String -> !l.startsWith(TAGS) }
                .filter { l: String -> !l.equals(workUnit.migration.trunk, ignoreCase = true) }
                .collect(Collectors.toList())
        }
        gitElementsToDelete.forEach(Consumer { s: String? -> LOG.debug(String.format("gitElementsToDelete(%s):%s",
            if (isTags) "tags" else "branches", s)) })

        // ######### List from branch or tag filter ##################################

        // branches or tags to keep according to filter applied if any (i.e. branches or tags to keep, everything else can be deleted)
        val keepListFromFilter: List<String>? = if (isTags)
            getListFromCommaSeparatedString(workUnit.migration.tagsToMigrate)
        else getListFromCommaSeparatedString(workUnit.migration.branchesToMigrate)

        // ######### List svn ls ##################################

        // List svn branch
        // In the case where no branch or tag filter has been applied.
        // The result of this svn ls command is a list of all branches or tags that are real (i.e. not deleted)
        // i) So if we remove these from gitElementsToDelete we should just have deleted branches or tags
        val svnUrl = if (workUnit.migration.svnUrl.endsWith("/")) workUnit.migration.svnUrl else String.format("%s/", workUnit.migration.svnUrl)
        val svnBranchList: String = if (StringUtils.isEmpty(workUnit.migration.svnPassword)) {
            String.format("svn ls %s%s%s/%s > %s", svnUrl, workUnit.migration.svnGroup, workUnit.migration.svnProject,
                if (isTags) "tags" else "branches", SVN_LIST)
        } else {
            String.format("svn ls %s%s%s/%s %s %s > %s", svnUrl,
                if (workUnit.migration.svnGroup.endsWith("/")) workUnit.migration.svnGroup else String.format("%s/", workUnit.migration.svnGroup),
                workUnit.migration.svnProject,
                if (isTags) "tags" else "branches",
                "--username=" + workUnit.migration.svnUser, "--password=" + workUnit.migration.svnPassword.escape(),
                SVN_LIST)
        }
        execCommand(workUnit.commandManager, workUnit.directory, svnBranchList)
        var elementsToKeep = Files.readAllLines(Paths.get(workUnit.directory, SVN_LIST))
            .stream()
            .map { l: String -> l.trim { it <= ' ' }.replace("/", "").encode() }
            .collect(Collectors.toList())

        // ######### Switch elementsToKeep if necessary ##################################
        if (keepListFromFilter != null && keepListFromFilter.isNotEmpty()) {
            elementsToKeep = keepListFromFilter
        }
        elementsToKeep.forEach(Consumer { s: String? -> LOG.debug(String.format("elementsToKeep(%s):%s", if (isTags) "tags" else "branches", s)) })

        // ######### See what needs to be deleted ##################################

        // Diff git & elementsToKeep
        gitElementsToDelete.removeAll(elementsToKeep)
        gitElementsToDelete.forEach(Consumer { s: String? -> LOG.debug(String.format("gitElements to delete(%s):%s", if (isTags) "tags" else "branches", s)) })

        // Remove none git branches
        gitElementsToDelete.forEach(Consumer { line: String ->
            try {
                var cleanCmd = String.format("git branch -d -r %s", String.format("\"origin/%s%s\"", if (isTags && !line.startsWith(TAGS)) TAGS else "", line))
                execCommand(workUnit.commandManager, workUnit.directory, cleanCmd)
                cleanCmd = if (Shell.isWindows) {
                    String.format("rd /s /q \".git\\svn\\refs\\remotes\\origin\\%s\\\"", String.format("%s%s", if (isTags) "tags\\" else "", line))
                } else {
                    // var mutableLine = line
                    // if (line.contains("(")) mutableLine = line.escapeParenthesis()
                    String.format("rm -rf %s", String.format("\".git/svn/refs/remotes/origin/%s%s\"", if (isTags && !line.startsWith(TAGS)) TAGS else "", line))
                }
                execCommand(workUnit.commandManager, workUnit.directory, cleanCmd)
            } catch (ex: IOException) {
                LOG.error("Cannot remove : $line")
                withWarning.set(true)
                failedBranches.add(line)
            } catch (ex: InterruptedException) {
                LOG.error("Cannot remove : $line")
                withWarning.set(true)
                failedBranches.add(line)
            } catch (ex: java.lang.RuntimeException) {
                LOG.error("Cannot remove : $line")
                withWarning.set(true)
                failedBranches.add(line)
            }
        })

        // Cleaning temp files
        var fileToDeletePath = Paths.get(workUnit.directory, GIT_LIST)
        Files.delete(fileToDeletePath)
        fileToDeletePath = Paths.get(workUnit.directory, SVN_LIST)
        Files.delete(fileToDeletePath)
        return Pair.of(withWarning, failedBranches)
    }

}
