package fr.yodamad.svn2git.service;

import com.madgag.git.bfg.cli.Main;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.Reason;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationRemovedFileRepository;
import fr.yodamad.svn2git.service.util.ArtifactoryAdmin;
import fr.yodamad.svn2git.service.util.ZipUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static fr.yodamad.svn2git.service.GitManager.*;
import static fr.yodamad.svn2git.service.util.Shell.execCommand;
import static fr.yodamad.svn2git.service.util.Shell.isWindows;
import static java.lang.String.format;
import static java.nio.file.Files.newDirectoryStream;
import static org.apache.commons.lang3.StringUtils.chop;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Cleaning operations
 */
@Service
public class Cleaner {

    private static final Logger LOG = LoggerFactory.getLogger(Cleaner.class);
    private static final String GIT_LIST = "git-list";
    private static final String SVN_LIST = "svn-list";
    private static final String TAGS = "tags/";

    // Manager
    private final HistoryManager historyMgr;
    // Repo
    private final MigrationRemovedFileRepository mrfRepo;
    // Artifactory properties
    private final ApplicationProperties.Artifactory artifactory;
    // Artifactory client
    private final ArtifactoryAdmin artifactoryAdmin;

    public Cleaner(final HistoryManager historyManager, final MigrationRemovedFileRepository repository,
                   final ApplicationProperties applicationProperties, final ArtifactoryAdmin artifactoryAdmin) {
        this.historyMgr = historyManager;
        this.mrfRepo = repository;
        this.artifactory = applicationProperties.artifactory;
        this.artifactoryAdmin = artifactoryAdmin;
    }

    /**
     * List files that are going to be cleaned by BFG
     * @param workUnit Current migration information
     * @throws IOException
     */
    void listCleanedFiles(WorkUnit workUnit) throws IOException, InterruptedException {

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.LIST_REMOVED_FILES, "");
        AtomicBoolean warnings = new AtomicBoolean(false);

        if (!isEmpty(workUnit.migration.getTrunk())) {
            listCleanedFilesInSvnLocation(workUnit, "trunk");
        }

        List<String> remotes = listRemotes(workUnit.directory);
        listBranchesOnly(remotes).forEach(
            b -> {
                try{
                    // get branchName
                    String branchName = b.replaceFirst("refs/remotes/origin/", "");
                    branchName = branchName.replaceFirst("origin/", "");
                    LOG.debug(format("Branch %s", branchName));
                    // checkout new branchName from existing remote branch
                    String gitCommand = format("git checkout -b %s %s", branchName, b);
                    execCommand(workUnit.directory, gitCommand);
                    // listCleanedFilesInSvnLocation
                    listCleanedFilesInSvnLocation(workUnit, b.replace("origin", "branches"));
                    // back to master
                    gitCommand = "git checkout master";
                    execCommand(workUnit.directory, gitCommand);
                    // delete the temporary branch
                    gitCommand = format("git branch -D %s", branchName);
                    execCommand(workUnit.directory, gitCommand);
                } catch (IOException | InterruptedException ioEx) {
                    LOG.warn(format("Failed to list removed files on %s", b));
                    warnings.set(true);
                }
            }
        );

        listTagsOnly(remotes).forEach(
            t -> {
                try{
                    // checkout new branch 'tmp_tag' from existing tag
                    String gitCommand = format("git checkout -b tmp_tag %s", t);
                    execCommand(workUnit.directory, gitCommand);
                    // listCleanedFilesInSvnLocation
                    listCleanedFilesInSvnLocation(workUnit, t.replace("origin", "tags"));
                    // back to master
                    gitCommand = "git checkout master";
                    execCommand(workUnit.directory, gitCommand);
                    // delete the temporary branch
                    gitCommand = "git branch -D tmp_tag";
                    execCommand(workUnit.directory, gitCommand);
                } catch (IOException | InterruptedException ioEx) {
                    LOG.warn(format("Failed to list removed files on %s", t));
                    warnings.set(true);
                }
            }
        );

        // back to master
        String gitCommand = "git checkout master";
        execCommand(workUnit.directory, gitCommand);

        // get list of files that will in principle be removed
        List<MigrationRemovedFile> migrationRemovedFiles = mrfRepo.findAllByMigration_Id(workUnit.migration.getId());
        String sMigrationRemovedFiles = migrationRemovedFiles.stream()
            .map(element -> format("(%s)/%s",element.getSvnLocation(),element.getPath()))
            .collect(Collectors.joining(", "));

        if (warnings.get()) {
            historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, sMigrationRemovedFiles);
        } else {
            historyMgr.endStep(history, StatusEnum.DONE, sMigrationRemovedFiles);
        }
    }

    /**
     * List files that are going to be cleaned by BFG
     * @param workUnit Current migration information
     * @throws IOException
     */
    void listCleanedFilesInSvnLocation(WorkUnit workUnit, String svnLocation) throws IOException {
        Path workingPath = Paths.get(workUnit.directory);
        inspectPath(workUnit, workingPath, svnLocation);
    }

    /**
     * Inspect current path (recursively)
     * @param workUnit Current migration information
     * @param workingPath Current path
     * @param svnLocation Current svn location (trunk, branch, ...)
     * @throws IOException
     */
    private void inspectPath(WorkUnit workUnit, Path workingPath, String svnLocation) throws IOException  {

        Path initialPath = Paths.get(workUnit.directory);
        DirectoryStream.Filter<Path> pathFilter = path -> {
            if (path.toFile().isDirectory()) {
                // Ignore git folder
                if (path.toFile().getName().equals(".git")) {
                    return false;
                }
                inspectPath(workUnit, path, svnLocation);
                return false;
            } else {
                return path.toFile().isFile()
                    && (isForbiddenExtension(workUnit, path) || exceedsMaxSize(workUnit, path));
            }
        };

        // If binaries directory, push binary to artifactory
        String completePath = workingPath.toFile().getCanonicalPath();
        if (artifactory.isEnabled()
            // Only tags for the moment
            // TODO : externalize to allow branches, trunk open configuration
            && svnLocation.startsWith("tags")
            && completePath.endsWith(artifactory.binariesDirectory)
            && workingPath.toFile().isDirectory()) {

            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.UPLOAD_TO_ARTIFACTORY, "");

            boolean containsSubDir = false;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(workingPath)) {
                for (Path p : stream) {
                    if (p.toFile().isDirectory()) {
                        containsSubDir = true;
                        break;
                    }
                }
            }

            if (containsSubDir) {

                // Zip before uploading
                String zipName = ZipUtil.zipDirectory(workingPath, svnLocation.replace(TAGS, ""));
                String artifactPath = artifactoryAdmin.uploadArtifact(
                    Paths.get(zipName).toFile(),
                    workUnit.migration.getSvnGroup(),
                    workUnit.migration.getSvnProject(),
                    svnLocation.replace(TAGS, ""));

                String pathForHistoryMgr = format("%s/%s", artifactory.binariesDirectory, zipName);

                // Remove file after uploading to avoid git commit
                Files.deleteIfExists(Paths.get(zipName));

                historyMgr.endStep(history, StatusEnum.DONE, format("Uploading Zip file to Artifactory : %s : %s", pathForHistoryMgr, artifactPath));

            } else {
                // Else upload all files to artifactory
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(workingPath)) {

                    StringBuilder artifactInfo = new StringBuilder();
                    for (Path p : stream) {
                        String artifactPath = artifactoryAdmin.uploadArtifact(
                            p.toFile(),
                            workUnit.migration.getSvnGroup(),
                            workUnit.migration.getSvnProject(),
                            svnLocation.replace(TAGS, ""));

                        String pathForHistoryMgr = format("%s/%s", artifactory.binariesDirectory, p.getFileName());

                        artifactInfo.append(format("%s : %s,", pathForHistoryMgr, artifactPath));
                    }

                    historyMgr.endStep(history, StatusEnum.DONE, (StringUtils.isEmpty(artifactInfo.toString()) ? "No Files Uploaded to Artifactory" : "Uploading file(s) to Artifactory, " + artifactInfo.toString()));

                }
            }
        }

        // List all future removed files
        try (DirectoryStream<Path> dirStream = newDirectoryStream(workingPath, pathFilter)) {
            for (Path p : dirStream) {
                Reason reason = isForbiddenExtension(workUnit, p) ? Reason.EXTENSION : Reason.SIZE;
                Long fileSize = getFileSize(p);
                MigrationRemovedFile mrf = new MigrationRemovedFile()
                    .migration(workUnit.migration)
                    .svnLocation(svnLocation)
                    .path(initialPath.relativize(p).toString())
                    .reason(reason)
                    .fileSize(fileSize);

                this.mrfRepo.save(mrf);
            }
        }

    }

    /**
     * Get File Size of the file found at path.
     * @param path The path of a file
     * @return
     */
    private Long getFileSize(Path path) {

        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            return -1L;
        } else {
            return Long.valueOf(file.length());
        }

    }

    /**
     * Check if current file has a forbidden extension (upper or lowercase)
     * @param workUnit Current migration information
     * @param path Current file
     * @return
     */
    private static boolean isForbiddenExtension(WorkUnit workUnit, Path path) {

        List<String> extensions = Arrays.stream(workUnit.migration.getForbiddenFileExtensions().
            split(",")).map(String::toLowerCase).collect(Collectors.toList());

        List<String> uppercaseExtensions = Arrays.stream(workUnit.migration.getForbiddenFileExtensions().
            split(",")).map(String::toUpperCase).collect(Collectors.toList());

        extensions.addAll(uppercaseExtensions);

        return extensions.stream()
            .anyMatch(ext -> path.toString().endsWith(ext.replaceFirst("\\*", "")));
    }

    /**
     * Check if current file exceeds max file size authorized
     * @param workUnit Current migration information
     * @param path Current file
     * @return
     * @throws IOException
     */
    private static boolean exceedsMaxSize(WorkUnit workUnit, Path path) throws IOException {
        if (!isEmpty(workUnit.migration.getMaxFileSize())
            && Character.isDigit(workUnit.migration.getMaxFileSize().charAt(0))) {
            String maxSize = workUnit.migration.getMaxFileSize();
            Long digits = Long.valueOf(chop(maxSize));
            String unit = maxSize.substring(maxSize.length() -1);
            switch (unit) {
                case "G":
                    digits = digits * 1024 * 1024 * 1024;
                case "M":
                    digits = digits * 1024 * 1024;
                case "K":
                    digits = digits * 1024;
                default:
                    break;
            }

            FileChannel imageFileChannel = FileChannel.open(path);

            boolean isImageFileChannel = imageFileChannel.size() > digits;
            imageFileChannel.close();

            return isImageFileChannel;

        }
        return false;
    }

    /**
     * Clean files with forbiddene extensions if configured
     * @param workUnit
     * @return
     */
    boolean cleanForbiddenExtensions(WorkUnit workUnit) throws IOException, InterruptedException {
        boolean clean = false;
        if (!isEmpty(workUnit.migration.getForbiddenFileExtensions())) {

            // needed?
            String gitCommand = "git gc";
            execCommand(workUnit.directory, gitCommand);

            // 3.1 Clean files based on their extensions
            Arrays.stream(workUnit.migration.getForbiddenFileExtensions().split(","))
                .forEach(s -> {
                    MigrationHistory innerHistory = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CLEANING, format("Remove files with extension : %s and %s", s.toLowerCase(), s.toUpperCase()));
                    try {
                        Main.main(new String[]{"--delete-files", s.toLowerCase(), "--no-blob-protection", workUnit.directory});
                        Main.main(new String[]{"--delete-files", s.toUpperCase(), "--no-blob-protection", workUnit.directory});
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
    boolean cleanLargeFiles(WorkUnit workUnit) throws IOException, InterruptedException {
        boolean clean = false;
        if (!isEmpty(workUnit.migration.getMaxFileSize()) && Character.isDigit(workUnit.migration.getMaxFileSize().charAt(0))) {
            // 3.2 Clean files based on size
            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CLEANING,
                format("Remove files bigger than %s", workUnit.migration.getMaxFileSize()));

            // This is necessary for BFG to work
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
     * Used to delete folder that is pushed to artifactory.
     * Note : This is only useful if the foldername is unique. Apparently not possible to provide a folder path.
     * Note : Needed to put this before other cleaning options (extension and size)
     *
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean cleanFolderWithBFG(WorkUnit workUnit) throws IOException, InterruptedException {
        boolean clean = false;
        if (artifactory.enabled && StringUtils.isNotEmpty(artifactory.deleteFolderWithBFG)) {
            // Delete folders based on a folder name
            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.ARTIFACTORY_FOLDER_CLEANING,
                format("Remove Folders called %s", artifactory.deleteFolderWithBFG));

            // needed?
            String gitCommand = "git gc";
            execCommand(workUnit.directory, gitCommand);

            Main.main(new String[]{
                "--delete-folders", "{" + artifactory.deleteFolderWithBFG + "}",
                "--no-blob-protection", workUnit.directory});

            clean = true;
            historyMgr.endStep(history, StatusEnum.DONE, null);
        }
        return clean;
    }

    /**
     * Clean elements removed in svn but clone in git svn
     * @param workUnit Current work unit
     * @param isTags Flag to know if it's tags processing
     * @return Pair containing warning flags & potential failed branches
     * @throws IOException
     * @throws InterruptedException
     */
    Pair<AtomicBoolean, List<String>> cleanRemovedElements(WorkUnit workUnit, boolean isTags) throws IOException, InterruptedException {
        AtomicBoolean withWarning = new AtomicBoolean();
        List<String> failedBranches = new ArrayList<>();
        // List git branch
        String gitBranchList = format("git branch -r > %s", GIT_LIST);
        execCommand(workUnit.directory, gitBranchList);

        List<String> gitElements;
        if (isTags) {
            gitElements = Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map(l -> l.trim().replace("origin/", ""))
                .filter(t -> t.startsWith("tags"))
                .map(l -> l.replace(TAGS, ""))
                .filter(l -> !l.equalsIgnoreCase("trunk"))
                .collect(Collectors.toList());
        } else {
            gitElements = Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map(l -> l.trim().replace("origin/", ""))
                .filter(l -> !l.startsWith(TAGS))
                .filter(l -> !l.equalsIgnoreCase("trunk"))
                .collect(Collectors.toList());
        }

        // List svn branch
        String svnUrl = workUnit.migration.getSvnUrl().endsWith("/") ? workUnit.migration.getSvnUrl() : format("%s/", workUnit.migration.getSvnUrl());
        String svnBranchList = format("svn ls %s%s/%s/%s > %s", svnUrl, workUnit.migration.getSvnGroup(), workUnit.migration.getSvnProject(), isTags ? "tags" : "branches", SVN_LIST);
        execCommand(workUnit.directory, svnBranchList);

        List<String> svnElements = Files.readAllLines(Paths.get(workUnit.directory, SVN_LIST))
            .stream()
            .map(l -> l.trim().replace("/", ""))
            .collect(Collectors.toList());

        // Diff git & svn branches
        gitElements.removeAll(svnElements);

        // Remove none git branches
        gitElements.forEach(line -> {
            try {
                String cleanCmd = format("git branch -d -r origin/%s", format("%s%s", isTags ? TAGS : "", line));
                execCommand(workUnit.directory, cleanCmd);

                if (isWindows) {
                    cleanCmd = format("rd /s /q .git\\svn\\refs\\remotes\\origin\\%s", format("%s%s", isTags ? "tags\\" : "", line));
                } else {
                    cleanCmd = format("rm -rf .git/svn/refs/remotes/origin/%s", format("%s%s", isTags ? TAGS : "", line));
                }

                execCommand(workUnit.directory, cleanCmd);
            } catch (IOException | InterruptedException ex) {
                LOG.error(format("Cannot remove : %s", line));
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
