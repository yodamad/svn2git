package fr.yodamad.svn2git.service;

import com.madgag.git.bfg.cli.Main;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static fr.yodamad.svn2git.service.util.Shell.execCommand;
import static java.lang.String.format;

/**
 * Cleaning operations
 */
@Service
public class Cleaner {

    private static final Logger LOG = LoggerFactory.getLogger(Cleaner.class);
    private static final String GIT_LIST = "git-list";
    private static final String SVN_LIST = "svn-list";
    private static final String DIFF_LIST = "diff-list";

    // Manager
    private final HistoryManager historyMgr;

    public Cleaner(final HistoryManager historyManager) {
        this.historyMgr = historyManager;
    }

    /**
     * Clean files with forbiddene extensions if configured
     * @param workUnit
     * @return
     */
    public boolean cleanForbiddenExtensions(WorkUnit workUnit) {
        boolean clean = false;
        if (!StringUtils.isEmpty(workUnit.migration.getForbiddenFileExtensions())) {
            // 3.1 Clean files based on their extensions
            Arrays.stream(workUnit.migration.getForbiddenFileExtensions().split(","))
                .forEach(s -> {
                    MigrationHistory innerHistory = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CLEANING, format("Remove files with extension : %s", s));
                    try {
                        Main.main(new String[]{"--delete-files", s, "--no-blob-protection", workUnit.directory});
                        historyMgr.endStep(innerHistory, StatusEnum.DONE, null);
                    } catch (Throwable exc) {
                        historyMgr.endStep(innerHistory, StatusEnum.FAILED, exc.getMessage());
                        workUnit.warnings.set(true);
                    }
                });
            clean = true;
        }
        return clean;
    }

    /**
     * Clean large files if configured
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean cleanLargeFiles(WorkUnit workUnit) throws IOException, InterruptedException {
        boolean clean = false;
        if (!StringUtils.isEmpty(workUnit.migration.getMaxFileSize()) && Character.isDigit(workUnit.migration.getMaxFileSize().charAt(0))) {
            // 3.2 Clean files based on size
            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CLEANING,
                format("Remove files bigger than %s", workUnit.migration.getMaxFileSize()));

            String gitCommand = "git gc";
            execCommand(workUnit.directory, gitCommand);

            Main.main(new String[]{
                "--strip-blobs-bigger-than", workUnit.migration.getMaxFileSize(),
                "--no-blob-protection", workUnit.directory});

            clean = true;
            historyMgr.endStep(history, StatusEnum.DONE, null);
        }
        return clean;
    }

    /**
     * Clean elements removed in svn but clone in git svn
     * @param workUnit Current work unit
     * @param tags Flag to know if it's tags processing
     * @return Pair containing warning flags & potential failed branches
     * @throws IOException
     * @throws InterruptedException
     */
    public Pair<AtomicBoolean, List<String>> cleanRemovedElements(WorkUnit workUnit, boolean tags) throws IOException, InterruptedException {
        AtomicBoolean withWarning = new AtomicBoolean();
        List<String> failedBranches = new ArrayList<>();
        // List git branch
        String gitBranchList = format("git branch -r > %s", GIT_LIST);
        execCommand(workUnit.directory, gitBranchList);

        List<String> gitElements;
        if (tags) {
            gitElements = Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map(l -> l.trim().replace("origin/", ""))
                .filter(t -> t.startsWith("tags"))
                .map(l -> l.replace("tags/", ""))
                .filter(l -> !l.equalsIgnoreCase("trunk"))
                .collect(Collectors.toList());
        } else {
            gitElements = Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map(l -> l.trim().replace("origin/", ""))
                .filter(l -> !l.startsWith("tags/"))
                .filter(l -> !l.equalsIgnoreCase("trunk"))
                .collect(Collectors.toList());
        }

        // List svn branch
        String svnUrl = workUnit.migration.getSvnUrl().endsWith("/") ? workUnit.migration.getSvnUrl() : format("%s/", workUnit.migration.getSvnUrl());
        String svnBranchList = format("svn ls %s%s/%s/%s > %s", svnUrl, workUnit.migration.getSvnGroup(), workUnit.migration.getSvnProject(), tags ? "tags" : "branches", SVN_LIST);
        execCommand(workUnit.directory, svnBranchList);

        List<String> svnElements = Files.readAllLines(Paths.get(workUnit.directory, SVN_LIST))
            .stream()
            .map(l -> l.trim().replace("/", ""))
            .collect(Collectors.toList());

        // Diff git & svn branches
        gitElements.removeAll(svnElements);

        // Remove none git branches
        gitElements.stream().forEach(line -> {
            try {
                String cleanCmd = format("git branch -d -r origin/%s", format("%s%s", tags ? "tags/" : "", line));
                execCommand(workUnit.directory, cleanCmd);
                cleanCmd = format("rm -rf .git/svn/refs/remotes/origin/%s", format("%s%s", tags ? "tags/" : "", line));
                execCommand(workUnit.directory, cleanCmd);
            } catch (IOException | InterruptedException ex) {
                LOG.error("Cannot remove : " + line);
                withWarning.set(true);
                failedBranches.add(line);
            }
        });

        // Cleaning temp files
        Path fileToDeletePath = Paths.get(workUnit.directory, GIT_LIST);
        Files.delete(fileToDeletePath);
        fileToDeletePath = Paths.get(workUnit.directory, SVN_LIST);
        Files.delete(fileToDeletePath);

        return Pair.of(withWarning, failedBranches);
    }
}
