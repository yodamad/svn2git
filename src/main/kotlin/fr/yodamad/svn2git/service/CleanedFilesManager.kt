package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.data.CleanedFiles
import fr.yodamad.svn2git.domain.enumeration.SvnLayout
import java.util.concurrent.atomic.AtomicLong

open class CleanedFilesManager(val cleanedReportMap: Map<String, CleanedFiles>) {

    /**
     *
     * @param beforeClean
     * @param svnLayout
     * @return true if there is at least one file in the trunk|branches|tags before|after the bfg cleaning process (calculated)
     */
    fun existsFileInSvnLayout(beforeClean: Boolean, svnLayout: SvnLayout): Boolean {
        return cleanedReportMap.entries.stream().filter { entry: Map.Entry<String, CleanedFiles> ->
            if (svnLayout == SvnLayout.ALL) true
            else entry.value.svnLayout == svnLayout }.anyMatch {
                entry: Map.Entry<String, CleanedFiles> ->
                    if (beforeClean) entry.value.isAtLeastOneFileBeforeClean
                    else entry.value.isAtLeastOneFileAfterClean
            }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        cleanedReportMap.entries.stream().forEach { entry: Map.Entry<String, CleanedFiles> ->
            sb.append(String.format("svnLocation:%s, totalFiles:%s, removedFiles:%s, remainingFiles:%s \n",
                entry.key, entry.value.fileCountBeforeClean, entry.value.deletedFileCountAfterClean, entry.value.fileCountAfterClean))
        }
        return sb.toString()
    }

    val fileSizeTotalBeforeClean: Long
        get() {
            val total = AtomicLong(0L)
            cleanedReportMap.entries.stream().forEach { entry: Map.Entry<String, CleanedFiles> -> total.addAndGet(entry.value.fileSizeTotalBeforeClean) }
            return total.get()
        }
    val fileSizeTotalAfterClean: Long
        get() {
            val total = AtomicLong(0L)
            cleanedReportMap.entries.stream().forEach { entry: Map.Entry<String, CleanedFiles> -> total.addAndGet(entry.value.fileSizeTotalAfterClean) }
            return total.get()
        }
}
