package fr.yodamad.svn2git.functions

import fr.yodamad.svn2git.domain.WorkUnit
import fr.yodamad.svn2git.service.GitManager
import fr.yodamad.svn2git.service.util.MigrationConstants
import fr.yodamad.svn2git.service.util.Shell
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.stream.Collectors


/**
 * Generate IgnoreRefs strings used as a parameter for git svn
 *
 * @param branchesToMigrate
 * @param tagsToMigrate
 * @return
 */
fun generateIgnoreRefs(branchesToMigrate: String?, tagsToMigrate: String?): String {

    // refs/remote/origin/branchname
    var branchesToMigrateList: List<String> = ArrayList()
    if (StringUtils.isNotBlank(branchesToMigrate)) {
        branchesToMigrateList = branchesToMigrate?.split(",")?.toTypedArray()?.toList()!!
    }
    var tagsToMigrateList: List<String> = ArrayList()
    if (StringUtils.isNotBlank(tagsToMigrate)) {
        tagsToMigrateList = tagsToMigrate?.split(",")?.toTypedArray()?.toList()!!
    }
    val sb = StringBuilder()
    if (branchesToMigrateList.isNotEmpty() || tagsToMigrateList.isNotEmpty()) {
        // examples
        // BranchANDTagProvided : --ignore-refs="^refs/remotes/origin/(?!branch1|tags/tag1).*$"
        // BranchONLYProvided   : --ignore-refs="^refs/remotes/origin/(?!branch1|tags/).*$"
        // TagONLYProvided      : --ignore-refs="^refs/remotes/origin/tags/(?!tag1).*$"
        if (branchesToMigrateList.isEmpty()) {
            // Keep all branches AND named tags
            sb.append("--ignore-refs=\"^refs/remotes/origin/tags/(?!")
        } else {
            // Keep named branches AND named tags
            // OR
            // Keep all tags AND named branches
            sb.append("--ignore-refs=\"^refs/remotes/origin/(?!")
        }
        if (branchesToMigrateList.isNotEmpty()) {
            val branchesIter = branchesToMigrateList.iterator()
            while (branchesIter.hasNext()) {
                val branch = branchesIter.next()
                sb.append(branch.trim { it <= ' ' }.replace(".", "\\."))
                sb.append("|")
            }
        }
        if (tagsToMigrateList.isNotEmpty()) {
            val tagsIter = tagsToMigrateList.iterator()
            while (tagsIter.hasNext()) {
                val tag = tagsIter.next()
                if (branchesToMigrateList.isNotEmpty()) {
                    sb.append("tags/")
                }
                sb.append(tag.trim { it <= ' ' }.replace(".", "\\."))
                if (tagsIter.hasNext()) {
                    sb.append("|")
                }
            }
        } else {
            sb.append("tags/")
        }
        sb.append(").*$\"")
        return sb.toString()
    }
    return ""
}

fun generateIgnorePaths(trunk: String?, tags: String?, branches: String?, svnProject: String, deleteSvnFolderList: List<String>?): String {

    // generate something like this
    if (deleteSvnFolderList == null || deleteSvnFolderList.isEmpty()) {
        return ""
    }
    val sb = java.lang.StringBuilder()
    sb.append("--ignore-paths=")

    // Whole expression surrounded by quotation marks
    sb.append("\"")

    // ********* START Dynamic Block SVN Paths ignored according to whether trunk, branches or tags selected
    sb.append("^(")
    if (trunk != null) {
        sb.append(svnProject.replaceFirst("/".toRegex(), "/?"))
            .append(String.format("/%s/", trunk))
        if (tags != null || branches != null) {
            sb.append("|")
        }
    }
    if (tags != null) {
        sb.append(svnProject.replaceFirst("/".toRegex(), "/?"))
            .append("/tags/[^/]+/")
        if (branches != null) {
            sb.append("|")
        }
    }

    // branches regex is just like trunk in fact
    if (branches != null) {
        sb.append(svnProject.replaceFirst("/".toRegex(), "/?"))
            .append("/branches/")
    }
    sb.append(")(")
    val it: Iterator<*> = deleteSvnFolderList.iterator()
    while (it.hasNext()) {
        sb.append(it.next())
            .append("/")
        if (it.hasNext()) {
            sb.append("|")
        }
    }
    sb.append(").*")

    // END Quotation marks
    sb.append("\"")

    // NOTE : Apparently can't select to ignore-path of trunk and keep same path of tag
    // (e.g. keep 05_impl/1_bin of tag only). Likely because tag is based on trunk (which has to exist)...
    return sb.toString()
}

/**
 * List SVN branches & tags cloned
 *
 * @param directory working directory
 * @return
 * @throws InterruptedException
 * @throws IOException
 */
@Throws(InterruptedException::class, IOException::class)
fun listRemotes(directory: String?): List<String> {
    val command = "git branch -r"
    val builder = ProcessBuilder()
    if (Shell.isWindows) {
        builder.command("cmd.exe", "/c", command)
    } else {
        builder.command("sh", "-c", command)
    }
    builder.directory(File(Shell.formatDirectory(directory)))
    val p = builder.start()
    val reader = BufferedReader(InputStreamReader(p.inputStream))
    val remotes: MutableList<String> = ArrayList()
    reader.lines().iterator().forEachRemaining { e: String -> remotes.add(e) }
    p.waitFor()
    p.destroy()
    return remotes
}

/**
 * List only branches
 *
 * @param remotes Remote list
 * @return list containing only branches
 */
fun listBranchesOnly(remotes: List<String>, trunk: String?): List<String>? = remotes.stream()
        .map { obj: String -> obj.trim { it <= ' ' } } // Remove tags
        .filter { b: String -> !b.startsWith(MigrationConstants.ORIGIN_TAGS) } // Remove master/trunk
        .filter { b: String -> !b.contains(MigrationConstants.MASTER) }
        .filter { b: String -> !b.contains(trunk!!) }
        .filter { b: String -> !b.contains("@") }
        .collect(Collectors.toList())

/**
 * List only tags
 *
 * @param remotes Remote list
 * @return list containing only tags
 */
fun listTagsOnly(remotes: List<String>): List<String>? = remotes.stream()
        .map { obj: String -> obj.trim { it <= ' ' } } // Only tags
        .filter { b: String -> b.startsWith(MigrationConstants.ORIGIN_TAGS) } // Remove temp tags
        .filter { b: String -> !b.contains("@") }
        .collect(Collectors.toList())

fun isFileInFolder(dirPath: String): Boolean {
    val log = LoggerFactory.getLogger(GitManager::class.java)
    var isFileInFolder: Boolean
    val files = File(dirPath).listFiles()
    if (files != null) {
        for (i in files.indices) {
            val file = files[i]

            // Only check subfolders if haven't found a file yet
            if (file.isDirectory) {
                if (!file.name.equals(".git", ignoreCase = true)) {
                    isFileInFolder = isFileInFolder(file.absolutePath)
                    if (isFileInFolder) {
                        return true
                    }
                } else {
                    log.info("Skipping check for files in .git folder")
                }
            } else {
                log.info("Found at least one file in this folder: " + file.absolutePath)
                return true
            }
        }
    }
    return false
}

fun buildTrunk(workUnit: WorkUnit): String? {
    val mig = workUnit.migration
    return if (mig.flat) {
        if (mig.svnGroup == mig.svnProject) {
            "--trunk=/"
        } else String.format("--trunk=%s/", workUnit.migration.svnProject)
    } else String.format("--trunk=%s/trunk", workUnit.migration.svnProject)
}
