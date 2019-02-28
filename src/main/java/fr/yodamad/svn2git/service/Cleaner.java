package fr.yodamad.svn2git.service;

import com.madgag.git.bfg.cli.Main;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.Reason;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationRemovedFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static fr.yodamad.svn2git.service.util.Shell.execCommand;
import static java.lang.String.format;
import static java.nio.file.Files.isRegularFile;
import static java.nio.file.Files.newDirectoryStream;

/**
 * Cleaning operations
 */
@Service
public class Cleaner {

    // Manager
    private final HistoryManager historyMgr;
    // Repo
    private final MigrationRemovedFileRepository mrfRepo;

    public Cleaner(final HistoryManager historyManager, final MigrationRemovedFileRepository repository) {
        this.historyMgr = historyManager;
        this.mrfRepo = repository;
    }

    /**
     * List files that are going to be cleaned by BFG
     * @param workUnit Current migration information
     * @throws IOException
     */
    public void listCleanedFiles(WorkUnit workUnit) throws IOException {
        Path workingPath = Paths.get(workUnit.directory);
        inspectPath(workUnit, workingPath);
    }

    /**
     * Inspect current path (recursively)
     * @param workUnit Current migration information
     * @param workingPath Current path
     * @throws IOException
     */
    private void inspectPath(WorkUnit workUnit, Path workingPath) throws IOException  {
        Path initialPath = Paths.get(workUnit.directory);
        DirectoryStream.Filter<Path> pathFilter = path -> {
            if (Files.isDirectory(path)) {
                inspectPath(workUnit, path);
                return false;
            } else {
                return isRegularFile(path) &&  isForbiddenExtension(workUnit, path);
            }
        };
        try (DirectoryStream<Path> dirStream = newDirectoryStream(workingPath, pathFilter)) {
            dirStream.forEach(p -> {
                MigrationRemovedFile mrf = new MigrationRemovedFile()
                    .migration(workUnit.migration)
                    .path(initialPath.relativize(p).toString())
                    .reason(Reason.EXTENSION);
                this.mrfRepo.save(mrf);
            });
        }
    }

    /**
     * Check if current file has a forbidden extension
     * @param workUnit Current migration information
     * @param path Current file
     * @return
     */
    private static boolean isForbiddenExtension(WorkUnit workUnit, Path path) {
        return Arrays.stream(workUnit.migration.getForbiddenFileExtensions().split(","))
            .anyMatch(ext -> path.toString().endsWith(ext.replaceFirst("\\*", "")));
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
                    } catch (Exception exc) {
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
}
