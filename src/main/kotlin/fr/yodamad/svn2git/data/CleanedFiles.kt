package fr.yodamad.svn2git.data

import fr.yodamad.svn2git.domain.enumeration.SvnLayout

class CleanedFiles(
    /**
     * trunk | branchname | tagname
     */
    val svnLocation: String,
    /**
     * Enum of the type of layout trunk | branch | tag | all
     */
    var svnLayout: SvnLayout) {
    /**
     * Total Number of Files before any cleanup
     */
    var fileCountBeforeClean = 0
    /**
     * Number of Files that will be deleted by bfg cleaning process (calculated)
     */
    var deletedFileCountAfterClean = 0
    /**
     * Remaining number of files after bfg cleaning process (calculated)
     */
    var fileCountAfterClean = 0
    /**
     * Used to calculate the size of files before the cleaning exercise.
     */
    var fileSizeTotalBeforeClean: Long = 0
    /**
     * Used to calculate the size of files after the cleaning exercise.
     */
    var fileSizeTotalAfterClean: Long = 0
    /**
     * convenience method
     * @return
     */
    val isAtLeastOneFileAfterClean: Boolean
        get() = fileCountAfterClean > 0
    /**
     * convenience method
     * @return
     */
    val isAtLeastOneFileBeforeClean: Boolean
        get() = fileCountBeforeClean > 0
    /**
     * convenience method
     * @return
     */
    val isAtLeastOneFileDeletedAfterClean: Boolean
        get() = deletedFileCountAfterClean > 0
}
