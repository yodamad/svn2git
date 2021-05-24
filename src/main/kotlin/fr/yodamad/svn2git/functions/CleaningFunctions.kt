package fr.yodamad.svn2git.functions

import fr.yodamad.svn2git.data.WorkUnit
import org.apache.commons.lang3.StringUtils
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

/**
 * Check if current file has a forbidden extension (upper or lowercase)
 *
 * @param workUnit Current migration information
 * @param path     Current file
 * @return
 */
fun isForbiddenExtension(workUnit: WorkUnit, path: Path): Boolean {
    if (workUnit.migration.forbiddenFileExtensions == null) return false
    val extensions = Arrays.stream(workUnit.migration.forbiddenFileExtensions.split(",").toTypedArray()).map { obj: String -> obj.toLowerCase() }.collect(Collectors.toList())
    val uppercaseExtensions = Arrays.stream(workUnit.migration.forbiddenFileExtensions.split(",").toTypedArray()).map { obj: String -> obj.toUpperCase() }.collect(Collectors.toList())
    extensions.addAll(uppercaseExtensions)
    return extensions.stream()
        .anyMatch { ext: String -> path.toString().endsWith(ext.replaceFirst("\\*".toRegex(), "")) }
}

/**
 * Check if current file exceeds max file size authorized
 *
 * @param workUnit Current migration information
 * @param path     Current file
 * @return
 * @throws IOException
 */
@Throws(IOException::class)
fun exceedsMaxSize(workUnit: WorkUnit, path: Path): Boolean {
    if (!StringUtils.isEmpty(workUnit.migration.maxFileSize)
        && Character.isDigit(workUnit.migration.maxFileSize[0])) {
        val maxSize = workUnit.migration.maxFileSize
        var digits = java.lang.Long.valueOf(StringUtils.chop(maxSize))
        val unit = maxSize.substring(maxSize.length - 1)
        when (unit) {
            "G" -> digits *= 1024 * 1024 * 1024
            "M" -> digits *= 1024 * 1024
            "K" -> digits *= 1024
            else -> { }
        }
        var isFileExceedsMaxSize: Boolean = false
        FileChannel.open(path).use { fileChannel -> isFileExceedsMaxSize = fileChannel.size() > digits }
        return isFileExceedsMaxSize
    }
    return false
}

/**
 * get List of strings from comma separated list of strings
 *
 * @param commaSeparatedStr
 * @return
 */
fun getListFromCommaSeparatedString(commaSeparatedStr: String?): List<String>? {
    return if (StringUtils.isNotBlank(commaSeparatedStr)) {
        commaSeparatedStr?.split(",")
    } else {
        ArrayList()
    }
}
