package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.enumeration.SvnLayout;

public class CleanedFiles {

    /**
     * trunk | branchname | tagname
     */
    public final String svnLocation;

    /**
     * Total Number of Files before any cleanup
     */
    public int fileCountBeforeClean;

    /**
     * Number of Files that will be deleted by bfg cleaning process (calculated)
     */
    public int deletedFileCountAfterClean;

    /**
     * Remaining number of files after bfg cleaning process (calculated)
     */
    public int fileCountAfterClean;

    /**
     * Used to calculate the size of files before the cleaning exercise.
     */
    public long fileSizeTotalBeforeClean;

    /**
     * Used to calculate the size of files after the cleaning exercise.
     */
    public long fileSizeTotalAfterClean;

    /**
     * Enum of the type of layout trunk | branch | tag | all
     */
    public SvnLayout svnLayout;

    public CleanedFiles(String svnLocation, SvnLayout svnLayout) {
        this.svnLocation = svnLocation;
        this.svnLayout = svnLayout;
    }

    /**
     * convenience method
     * @return
     */
    public boolean isAtLeastOneFileAfterClean() {
        return fileCountAfterClean > 0;
    }

    /**
     * convenience method
     * @return
     */
    public Boolean isAtLeastOneFileBeforeClean() {
        return fileCountBeforeClean > 0;
    }

    /**
     * convenience method
     * @return
     */
    public Boolean isAtLeastOneFileDeletedAfterClean() {
        return deletedFileCountAfterClean > 0;
    }

    /**
     * Gets svnLocation
     *
     * @return value of svnLocation
     */
    public String getSvnLocation() {
        return svnLocation;
    }

    /**
     * Gets fileCountBeforeClean
     *
     * @return value of fileCountBeforeClean
     */
    public int getFileCountBeforeClean() {
        return fileCountBeforeClean;
    }

    /**
     * Set the fileCountBeforeClean.
     *
     * @param fileCountBeforeClean
     */
    public void setFileCountBeforeClean(int fileCountBeforeClean) {
        this.fileCountBeforeClean = fileCountBeforeClean;
    }

    /**
     * Gets deletedFileCountAfterClean
     *
     * @return value of deletedFileCountAfterClean
     */
    public int getDeletedFileCountAfterClean() {
        return deletedFileCountAfterClean;
    }

    /**
     * Set the deletedFileCountAfterClean.
     *
     * @param deletedFileCountAfterClean
     */
    public void setDeletedFileCountAfterClean(int deletedFileCountAfterClean) {
        this.deletedFileCountAfterClean = deletedFileCountAfterClean;
    }

    /**
     * Gets fileCountAfterClean
     *
     * @return value of fileCountAfterClean
     */
    public int getFileCountAfterClean() {
        return fileCountAfterClean;
    }

    /**
     * Set the fileCountAfterClean.
     *
     * @param fileCountAfterClean
     */
    public void setFileCountAfterClean(int fileCountAfterClean) {
        this.fileCountAfterClean = fileCountAfterClean;
    }

    /**
     * Gets fileSizeTotalBeforeClean
     *
     * @return value of fileSizeTotalBeforeClean
     */
    public long getFileSizeTotalBeforeClean() {
        return fileSizeTotalBeforeClean;
    }

    /**
     * Set the fileSizeTotalBeforeClean.
     *
     * @param fileSizeTotalBeforeClean
     */
    public void setFileSizeTotalBeforeClean(long fileSizeTotalBeforeClean) {
        this.fileSizeTotalBeforeClean = fileSizeTotalBeforeClean;
    }

    /**
     * Gets fileSizeTotalAfterClean
     *
     * @return value of fileSizeTotalAfterClean
     */
    public long getFileSizeTotalAfterClean() {
        return fileSizeTotalAfterClean;
    }

    /**
     * Set the fileSizeTotalAfterClean.
     *
     * @param fileSizeTotalAfterClean
     */
    public void setFileSizeTotalAfterClean(long fileSizeTotalAfterClean) {
        this.fileSizeTotalAfterClean = fileSizeTotalAfterClean;
    }

    /**
     * Gets svnLayout
     *
     * @return value of svnLayout
     */
    public SvnLayout getSvnLayout() {
        return svnLayout;
    }

    /**
     * Set the svnLayout.
     *
     * @param svnLayout
     */
    public void setSvnLayout(SvnLayout svnLayout) {
        this.svnLayout = svnLayout;
    }
}
