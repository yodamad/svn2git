package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.enumeration.SvnLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

public class CleanedFilesManager {

    private static final Logger LOG = LoggerFactory.getLogger(CleanedFilesManager.class);

    private final Map<String, CleanedFiles> cleanedReportMap;

    public CleanedFilesManager(Map<String, CleanedFiles> cleanedReportMap) {
        this.cleanedReportMap = cleanedReportMap;
    }

    /**
     *
     * @param beforeClean
     * @param svnLayout
     * @return true if there is at least one file in the trunk|branches|tags before|after the bfg cleaning process (calculated)
     */
    public boolean existsFileInSvnLayout(boolean beforeClean, SvnLayout svnLayout) {

        boolean existsFileAfterClean = cleanedReportMap.
            entrySet().
            stream().
            filter(entry -> svnLayout.equals(SvnLayout.ALL) ? true : entry.getValue().svnLayout.equals(svnLayout)).
            anyMatch(entry -> Boolean.TRUE.equals((beforeClean ? entry.getValue().isAtLeastOneFileBeforeClean() : entry.getValue().isAtLeastOneFileAfterClean())));

        return  existsFileAfterClean;
    }


    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        cleanedReportMap.
            entrySet().
            stream().
            forEach(entry -> sb.append(format("svnLocation:%s, totalFiles:%s, removedFiles:%s, remainingFiles:%s \n",
                entry.getKey(), entry.getValue().fileCountBeforeClean, entry.getValue().deletedFileCountAfterClean, entry.getValue().fileCountAfterClean)));

        return sb.toString();
    }

    public long getFileSizeTotalBeforeClean() {

        AtomicLong total = new AtomicLong(0L);

        cleanedReportMap.
            entrySet().
            stream().
            forEach(entry -> total.addAndGet(entry.getValue().fileSizeTotalBeforeClean));

        return total.get();
    }

    public long getFileSizeTotalAfterClean() {

        AtomicLong total = new AtomicLong(0L);

        cleanedReportMap.
            entrySet().
            stream().
            forEach(entry -> total.addAndGet(entry.getValue().fileSizeTotalAfterClean));

        return total.get();

    }

    /**
     * Gets cleanedReportMap
     *
     * @return value of cleanedReportMap
     */
    public Map<String, CleanedFiles> getCleanedReportMap() {
        return cleanedReportMap;
    }
}
