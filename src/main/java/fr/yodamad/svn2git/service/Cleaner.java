package fr.yodamad.svn2git.service;

import com.madgag.git.bfg.cli.Main;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.Reason;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.domain.enumeration.SvnLayout;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static fr.yodamad.svn2git.service.GitManager.listBranchesOnly;
import static fr.yodamad.svn2git.service.GitManager.listRemotes;
import static fr.yodamad.svn2git.service.GitManager.listTagsOnly;
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
     * Check if current file has a forbidden extension (upper or lowercase)
     *
     * @param workUnit Current migration information
     * @param path     Current file
     * @return
     */
    private static boolean isForbiddenExtension(WorkUnit workUnit, Path path) {

        if (workUnit.migration.getForbiddenFileExtensions() == null) return false;

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
     *
     * @param workUnit Current migration information
     * @param path     Current file
     * @return
     * @throws IOException
     */
    private static boolean exceedsMaxSize(WorkUnit workUnit, Path path) throws IOException {
        if (!isEmpty(workUnit.migration.getMaxFileSize())
            && Character.isDigit(workUnit.migration.getMaxFileSize().charAt(0))) {
            String maxSize = workUnit.migration.getMaxFileSize();
            Long digits = Long.valueOf(chop(maxSize));
            String unit = maxSize.substring(maxSize.length() - 1);
            switch (unit) {
                case "G":
                    digits = digits * 1024 * 1024 * 1024;
                    break;
                case "M":
                    digits = digits * 1024 * 1024;
                    break;
                case "K":
                    digits = digits * 1024;
                    break;
                default:
                    break;
            }


            boolean isFileExceedsMaxSize;
            // try-with-resources closes automatically
            try (FileChannel fileChannel = FileChannel.open(path)) {
                isFileExceedsMaxSize = fileChannel.size() > digits;
            }

            return isFileExceedsMaxSize;

        }
        return false;
    }

    /**
     * get List of strings from comma separated list of strings
     *
     * @param commaSeparatedStr
     * @return
     */
    public static List<String> getListFromCommaSeparatedString(String commaSeparatedStr) {
        if (StringUtils.isNotBlank(commaSeparatedStr)) {
            String[] commaSeparatedArr = commaSeparatedStr.split("\\s*,\\s*");
            List<String> result = Arrays.stream(commaSeparatedArr).collect(Collectors.toList());
            return result;
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * List files that are going to be cleaned by BFG
     *
     * @param workUnit Current migration information
     * @throws IOException
     */
    public CleanedFilesManager listCleanedFiles(WorkUnit workUnit) throws IOException, InterruptedException {

        Map<String, CleanedFiles> cleanedFilesMap = new LinkedHashMap<>();

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.LIST_REMOVED_FILES, "");
        AtomicBoolean warnings = new AtomicBoolean(false);

        if (!isEmpty(workUnit.migration.getTrunk())) {
            CleanedFiles cleanedFilesTrunk = listCleanedFilesInSvnLocation(workUnit, workUnit.migration.getTrunk(), SvnLayout.TRUNK);
            cleanedFilesMap.put(workUnit.migration.getTrunk(), cleanedFilesTrunk);
        }

        List<String> remotes = listRemotes(workUnit.directory);
        listBranchesOnly(remotes, workUnit.migration.getTrunk()).forEach(
            b -> {
                try {
                    // get branchName
                    String branchName = b.replaceFirst("refs/remotes/origin/", "");
                    branchName = branchName.replaceFirst("origin/", "");
                    LOG.debug(format("Branch %s", branchName));
                    // checkout new branchName from existing remote branch
                    String gitCommand = format("git checkout -b %s %s", branchName, b);
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                    // listCleanedFilesInSvnLocation
                    CleanedFiles cleanedFilesBranch =
                        listCleanedFilesInSvnLocation(workUnit, b.replace("origin", "branches"), SvnLayout.BRANCH);
                    cleanedFilesMap.put(b.replace("origin", "branches"), cleanedFilesBranch);
                    // back to master
                    gitCommand = "git checkout master";
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                    // delete the temporary branch
                    gitCommand = format("git branch -D %s", branchName);
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                } catch (IOException | InterruptedException ioEx) {
                    LOG.warn(format("Failed to list removed files on %s", b));
                    warnings.set(true);
                }
            }
        );

        listTagsOnly(remotes).forEach(
            t -> {
                try {
                    // checkout new branch 'tmp_tag' from existing tag
                    String gitCommand = format("git checkout -b tmp_tag %s", t);
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                    // listCleanedFilesInSvnLocation
                    CleanedFiles cleanedFilesTag =
                        listCleanedFilesInSvnLocation(workUnit, t.replace("origin", "tags"), SvnLayout.TAG);
                    cleanedFilesMap.put(t.replace("origin", "tags"), cleanedFilesTag);
                    // back to master
                    gitCommand = "git checkout master";
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                    // delete the temporary branch
                    gitCommand = "git branch -D tmp_tag";
                    execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                } catch (IOException | InterruptedException ioEx) {
                    LOG.warn(format("Failed to list removed files on %s", t));
                    warnings.set(true);
                }
            }
        );

        // back to master
        String gitCommand = "git checkout master";
        execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

        // get list of files that will in principle be removed
        List<MigrationRemovedFile> migrationRemovedFiles = mrfRepo.findAllByMigration_Id(workUnit.migration.getId());
        String sMigrationRemovedFiles = migrationRemovedFiles.stream()
            .map(element -> format("(%s)/%s", element.getSvnLocation(), element.getPath()))
            .collect(Collectors.joining(", "));

        if (warnings.get()) {
            historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, sMigrationRemovedFiles);
        } else {
            historyMgr.endStep(history, StatusEnum.DONE, sMigrationRemovedFiles);
        }

        CleanedFilesManager cleanedFilesManager = new CleanedFilesManager(cleanedFilesMap);

        return cleanedFilesManager;
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
    CleanedFiles listCleanedFilesInSvnLocation(WorkUnit workUnit, String svnLocation, SvnLayout svnLayout) throws IOException {

        CleanedFiles cleanedFiles = new CleanedFiles(svnLocation, svnLayout);

        Path workingPath = Paths.get(workUnit.directory);
        inspectPath(workUnit, workingPath, svnLocation, cleanedFiles);

        return cleanedFiles;
    }

    /**
     * Inspect current path (recursively)
     *
     * @param workUnit    Current migration information
     * @param workingPath Current path
     * @param svnLocation Current svn location (trunk, branch, ...)
     * @throws IOException
     */
    private void inspectPath(WorkUnit workUnit, Path workingPath, String svnLocation, CleanedFiles cleanedFiles) throws IOException {

        Path initialPath = Paths.get(workUnit.directory);
        DirectoryStream.Filter<Path> pathFilter = path -> {
            if (path.toFile().isDirectory()) {
                // Ignore git folder
                if (path.toFile().getName().equals(".git")) {
                    return false;
                }
                inspectPath(workUnit, path, svnLocation, cleanedFiles);
                return false;
            } else {

                File file = path.toFile();

                boolean isForbiddenExtension = isForbiddenExtension(workUnit, path);
                boolean exceedsMaxSize = exceedsMaxSize(workUnit, path);
                boolean isToBeDeleted = file.isFile()
                    && (isForbiddenExtension || exceedsMaxSize);


                if (file.isFile()) {
                    cleanedFiles.fileCountBeforeClean++;
                    cleanedFiles.fileSizeTotalBeforeClean += file.length();
                    if (isToBeDeleted) {
                        cleanedFiles.deletedFileCountAfterClean++;
                    } else {
                        cleanedFiles.fileCountAfterClean++;
                        cleanedFiles.fileSizeTotalAfterClean += file.length();
                    }
                }

                return isToBeDeleted;
            }
        };

        // If binaries directory, push binary to artifactory
        String completePath = workingPath.toFile().getCanonicalPath();
        if (artifactory.isEnabled()
            // Only tags for the moment
            // TODO : externalize to allow branches, trunk open configuration
            && svnLocation.startsWith("tags")
            && completePath.endsWith(workUnit.migration.getSvnGroup() + artifactory.binariesDirectory)
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

                String zipPath = null;
                try {
                    // Zip before uploading
                    zipPath = ZipUtil.zipDirectory(workUnit, workingPath, svnLocation.replace(TAGS, ""));
                    String artifactPath = artifactoryAdmin.uploadArtifact(
                        Paths.get(zipPath).toFile(),
                        workUnit.migration.getSvnGroup(),
                        workUnit.migration.getSvnProject(),
                        svnLocation.replace(TAGS, ""));

                    historyMgr.endStep(history, StatusEnum.DONE, format("Uploading Zip file to Artifactory : %s : %s", zipPath, artifactPath));

                } finally {
                    if (zipPath != null) {
                        // Remove file after uploading to avoid git commit
                        Files.deleteIfExists(Paths.get(zipPath));
                    }
                }


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
     *
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
     * Clean files with forbiddene extensions if configured
     *
     * @param workUnit
     * @return
     */
    boolean cleanForbiddenExtensions(WorkUnit workUnit) throws IOException, InterruptedException {
        boolean clean = false;
        if (!isEmpty(workUnit.migration.getForbiddenFileExtensions())) {

            // needed?
            String gitCommand = "git gc";
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

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
     *
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
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

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
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

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
     *
     * @param workUnit Current work unit
     * @param isTags   Flag to know if it's tags processing
     * @return Pair containing warning flags & potential failed branches
     * @throws IOException
     * @throws InterruptedException
     */
    Pair<AtomicBoolean, List<String>> cleanRemovedElements(WorkUnit workUnit, boolean isTags) throws IOException, InterruptedException {
        AtomicBoolean withWarning = new AtomicBoolean();
        List<String> failedBranches = new ArrayList<>();

        // ######### List git branch ##################################

        // The exectution of "git branch -r" will give us a list of the branches or tags that were really phyically "clone"
        //     In the command "git svn clone"
        // This should normally correspond to the requested branches and tags (through use of branch or tag filter)
        //    However the ignore-refs configuration fails for certain project(s) - non identified issue.
        // Also if no branch or tag filter was indicated you may have 'deleted' branches or tags.
        String gitBranchList = format("git branch -r > %s", GIT_LIST);
        execCommand(workUnit.commandManager, workUnit.directory, gitBranchList);

        List<String> gitElementsToDelete;
        if (isTags) {
            gitElementsToDelete = Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map(l -> l.trim().replace("origin/", ""))
                .filter(t -> t.startsWith("tags"))
                .map(l -> l.replace(TAGS, ""))
                .filter(l -> !l.equalsIgnoreCase(workUnit.migration.getTrunk()))
                .collect(Collectors.toList());
        } else {
            gitElementsToDelete = Files.readAllLines(Paths.get(workUnit.directory, GIT_LIST))
                .stream()
                .map(l -> l.trim().replace("origin/", ""))
                .filter(l -> !l.startsWith(TAGS))
                .filter(l -> !l.equalsIgnoreCase(workUnit.migration.getTrunk()))
                .collect(Collectors.toList());
        }

        gitElementsToDelete.forEach(s -> LOG.debug(format("gitElementsToDelete(%s):%s", (isTags ? "tags" : "branches"), s)));

        // ######### List from branch or tag filter ##################################

        // branches or tags to keep according to filter applied if any (i.e. branches or tags to keep, everything else can be deleted)
        List<String> keepListFromFilter = (isTags ? getListFromCommaSeparatedString(workUnit.migration.getTagsToMigrate()) : getListFromCommaSeparatedString(workUnit.migration.getBranchesToMigrate()));

        // ######### List svn ls ##################################

        // List svn branch
        // In the case where no branch or tag filter has been applied.
        // The result of this svn ls command is a list of all branches or tags that are real (i.e. not deleted)
        // i) So if we remove these from gitElementsToDelete we should just have deleted branches or tags
        String svnUrl = workUnit.migration.getSvnUrl().endsWith("/") ? workUnit.migration.getSvnUrl() : format("%s/", workUnit.migration.getSvnUrl());
        String svnBranchList = format("svn ls %s%s%s/%s > %s", svnUrl, workUnit.migration.getSvnGroup(), workUnit.migration.getSvnProject(), isTags ? "tags" : "branches", SVN_LIST);
        execCommand(workUnit.commandManager, workUnit.directory, svnBranchList);

        List<String> elementsToKeep = Files.readAllLines(Paths.get(workUnit.directory, SVN_LIST))
            .stream()
            .map(l -> l.trim().replace("/", ""))
            .collect(Collectors.toList());

        // ######### Switch elementsToKeep if necessary ##################################

        if (!keepListFromFilter.isEmpty()) {
            elementsToKeep = keepListFromFilter;
        }

        elementsToKeep.forEach(s -> LOG.debug(format("elementsToKeep(%s):%s", (isTags ? "tags" : "branches"), s)));

        // ######### See what needs to be deleted ##################################

        // Diff git & elementsToKeep
        gitElementsToDelete.removeAll(elementsToKeep);

        gitElementsToDelete.forEach(s -> LOG.debug(format("gitElements to delete(%s):%s", (isTags ? "tags" : "branches"), s)));

        // Remove none git branches
        gitElementsToDelete.forEach(line -> {
            try {
                String cleanCmd = format("git branch -d -r origin/%s", format("%s%s", isTags ? TAGS : "", line));
                execCommand(workUnit.commandManager, workUnit.directory, cleanCmd);

                if (isWindows) {
                    cleanCmd = format("rd /s /q .git\\svn\\refs\\remotes\\origin\\%s", format("%s%s", isTags ? "tags\\" : "", line));
                } else {
                    cleanCmd = format("rm -rf .git/svn/refs/remotes/origin/%s", format("%s%s", isTags ? TAGS : "", line));
                }

                execCommand(workUnit.commandManager, workUnit.directory, cleanCmd);
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
