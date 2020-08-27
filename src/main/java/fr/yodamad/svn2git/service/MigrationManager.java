package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.config.Constants;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.domain.enumeration.SvnLayout;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.util.CommandManager;
import fr.yodamad.svn2git.service.util.MarkdownGenerator;
import fr.yodamad.svn2git.service.util.Shell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static fr.yodamad.svn2git.service.util.MigrationConstants.GIT_PUSH;
import static fr.yodamad.svn2git.service.util.MigrationConstants.MASTER;
import static fr.yodamad.svn2git.service.util.Shell.*;
import static java.lang.String.format;

/**
 * Migration manager processing all steps
 */
@Service
public class MigrationManager {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationManager.class);

    /**
     * Gitlab API.
     */

    // Managers
    private final HistoryManager historyMgr;
    private final GitManager gitManager;
    private final Cleaner cleaner;
    // Repositories
    private final MigrationRepository migrationRepository;
    private final MigrationHistoryRepository migrationHistoryRepository;
    // Configuration
    private final ApplicationProperties applicationProperties;
    // MarkdownGenerator
    private final MarkdownGenerator markdownGenerator;

    public MigrationManager(final Cleaner cleaner,
                            final GitManager gitManager,
                            final HistoryManager historyManager,
                            final MigrationRepository migrationRepository,
                            final MigrationHistoryRepository migrationHistoryRepository,
                            final ApplicationProperties applicationProperties,
                            final MarkdownGenerator markdownGenerator) {
        this.cleaner = cleaner;
        this.gitManager = gitManager;
        this.historyMgr = historyManager;
        this.migrationRepository = migrationRepository;
        this.migrationHistoryRepository = migrationHistoryRepository;
        this.applicationProperties = applicationProperties;
        this.markdownGenerator = markdownGenerator;
    }

    /**
     * Start a migration in a dedicated thread
     *
     * @param migrationId ID for migration to start
     * @param retry       Flag to know if it's the first attempt or a retry
     */
    @Async
    public Future<String> startMigration(final long migrationId, final boolean retry) {
        String gitCommand;
        Migration migration = migrationRepository.findById(migrationId).
            orElseThrow(NoSuchElementException::new);
        MigrationHistory history = null;
        String rootDir;

        CommandManager commandManager = new CommandManager(migration);

        try {
            history = historyMgr.startStep(migration, StepEnum.INIT,
                (commandManager.isFirstAttemptMigration() ? "" : Constants.REEXECUTION_SKIPPING) + "Create working directory");
            // If migration.workingDirectory is set we are reexecuting a 'partial' migration
            if (commandManager.isFirstAttemptMigration()) {
                rootDir = workingDir(commandManager, applicationProperties.work.directory, migration);
            } else {
                rootDir = commandManager.getWorkingDirectoryPath();
            }
            historyMgr.endStep(history, StatusEnum.DONE, null);
        } catch (IOException | InterruptedException | RuntimeException ex) {
            historyMgr.endStep(history, StatusEnum.FAILED, format("Failed to create directory : %s", ex.getMessage()));
            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
            return new AsyncResult<>("KO");
        }

        WorkUnit workUnit = new WorkUnit(migration, Shell.formatDirectory(rootDir),
            gitWorkingDir(rootDir, migration.getSvnGroup()), new AtomicBoolean(false), commandManager);

        try {

            // Start migration
            migration.setStatus(StatusEnum.RUNNING);
            migrationRepository.save(migration);

            // 1. Create project on gitlab : OK
            gitManager.createGitlabProject(migration, workUnit);

            // If reexecution we initialise from clean copy.
            initRootDirectoryFromCopy(workUnit);

            // 2. Checkout empty repository : OK
            String svn = initDirectory(workUnit);

            // 2.1 Avoid implicit git gc that may trigger error: fatal: gc is already running on machine '<servername>' pid 124077 (use --force if not)
            //     Note: Git GC will be triggered following git svn clone (on large projects) which causes a crash in following steps.
            history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_CONFIG_GLOBAL_GC_AUTO_OFF, "Assure Git Garbage Collection doesn't run in background to avoid conflicts.");
            gitCommand = "git config --global gc.auto 0";
            execCommand(commandManager, workUnit.directory, gitCommand);
            historyMgr.endStep(history, StatusEnum.DONE, null);

            // Log all git config before
            logGitConfig(workUnit);
            logUlimit(workUnit);
            // Log default character encoding
            LOG.info("Charset.defaultCharset().displayName():" + Charset.defaultCharset().displayName());

            // 2.2. SVN checkout
            gitManager.gitSvnClone(workUnit);
            checkGitConfig(workUnit);

            copyRootDirectory(workUnit);
            // Migration is now reexecutable in cases where there is a failure
            commandManager.setReexecutable(true);

            // Apply dynamic local configuration
            applicationProperties.
                getGitlab().
                getDynamicLocalConfig().
                stream().
                map(s -> s.split(",")).
                collect(Collectors.toMap(a -> a[0].trim(), a -> a[1].trim())).
                forEach((key, value) -> {
                    try {
                        addDynamicLocalConfig(workUnit, key, value);
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });

            // 2.3. Remove phantom branches
            if (migration.getBranches() != null) {
                history = historyMgr.startStep(migration, StepEnum.BRANCH_CLEAN,"Clean removed SVN branches");
                Pair<AtomicBoolean, List<String>> pairInfo = cleaner.cleanRemovedElements(workUnit, false);
                if (pairInfo.getFirst().get()) {
                    //  Some branches have failed
                    historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, format("Failed to remove branches %s", pairInfo.getSecond()));
                } else {
                    historyMgr.endStep(history, StatusEnum.DONE, null);
                }
            }

            // 2.4. Remove phantom tags
            if (migration.getTags() != null) {
                history = historyMgr.startStep(migration, StepEnum.TAG_CLEAN,"Clean removed SVN tags");
                Pair<AtomicBoolean, List<String>> pairInfo = cleaner.cleanRemovedElements(workUnit, true);
                if (pairInfo.getFirst().get()) {
                    //  Some branches have failed
                    historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, format("Failed to remove tags %s", pairInfo.getSecond()));
                } else {
                    historyMgr.endStep(history, StatusEnum.DONE, null);
                }
            }

            // 3. Clean files
            // 3.1 List files to remove. uploads binaries to Artifactory.
            CleanedFilesManager cleanedFilesManager = cleaner.listCleanedFiles(workUnit);
            LOG.info(cleanedFilesManager.toString());

            // Only launch clean steps if there is a file to clean in trunk, branches or tags.
            // If no files at this stage, no migration is executed i.e. no push to gitlab.
            if (cleanedFilesManager.existsFileInSvnLayout(true, SvnLayout.ALL)) {

                // 3.2 Remove
                boolean cleanFolderWithBFG = cleaner.cleanFolderWithBFG(workUnit);
                boolean cleanExtensions = cleaner.cleanForbiddenExtensions(workUnit);
                boolean cleanLargeFiles = cleaner.cleanLargeFiles(workUnit);


                if (cleanExtensions || cleanLargeFiles || cleanFolderWithBFG) {
                    gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive";
                    execCommand(commandManager, workUnit.directory, gitCommand);
                    gitCommand = "git reset HEAD";
                    execCommand(commandManager, workUnit.directory, gitCommand);
                    gitCommand = "git clean -fd";
                    execCommand(commandManager, workUnit.directory, gitCommand);
                }

                // 4. Git push master based on SVN trunk
                if (migration.getTrunk() != null) {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, format("SVN %s -> GitLab master", migration.getTrunk()));

                    // Set origin
                    execCommand(commandManager, workUnit.directory,
                        gitManager.buildRemoteCommand(workUnit, svn, false),
                        gitManager.buildRemoteCommand(workUnit, svn, true));

                    if (!migration.getTrunk().equals("trunk")) {
                        gitCommand = format("git checkout -b %s %s", migration.getTrunk(), "refs/remotes/origin/" + migration.getTrunk());
                        execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                        gitCommand = format("git branch -D master");
                        execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                        gitCommand = format("git branch -m master");
                        execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                    }

                    // if no history option set
                    if (migration.getSvnHistory().equals("nothing")) {
                        gitManager.removeHistory(workUnit, MASTER, false, history);
                    } else {
                        // Push with upstream
                        gitCommand = format("%s --set-upstream origin master", GIT_PUSH);
                        execCommand(commandManager, workUnit.directory, gitCommand);
                        historyMgr.endStep(history, StatusEnum.DONE, null);
                    }

                    // Clean pending file(s) removed by BFG
                    gitCommand = "git reset --hard origin/master";
                    execCommand(commandManager, workUnit.directory, gitCommand);

                    // 5. Apply mappings if some
                    boolean warning = gitManager.applyMapping(workUnit, MASTER);
                    workUnit.warnings.set(workUnit.warnings.get() || warning);
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, migration.getTrunk());
                    historyMgr.endStep(history, StatusEnum.IGNORED, format("Skip %s", migration.getTrunk()));
                }

                // 6. List branches & tags
                List<String> remotes = GitManager.listRemotes(workUnit.directory);
                // Extract branches
                if (!StringUtils.isEmpty(migration.getBranches()) && migration.getBranches().equals("*")) {
                    gitManager.manageBranches(workUnit, remotes);
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Branches");
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip branches");
                }

                // Extract tags
                if (!StringUtils.isEmpty(migration.getTags()) && migration.getTags().equals("*")) {
                    gitManager.manageTags(workUnit, remotes);
                } else {
                    history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Tags");
                    historyMgr.endStep(history, StatusEnum.IGNORED, "Skip tags");
                }

                // Generate summary
                try {
                    history = historyMgr.startStep(migration, StepEnum.README_MD, "Generate README.md to summarize migration");
                    gitCommand = "git checkout master";
                    execCommand(commandManager, workUnit.directory, gitCommand);

                    // If master not migrated, clean it to add only README.md
                    if (migration.getTrunk() == null) {
                        Arrays.stream(new File(workUnit.directory).listFiles())
                            .filter(f -> !f.getName().equalsIgnoreCase(".git"))
                            .forEach(f -> {
                                try {
                                    FileUtils.forceDelete(f);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        gitCommand = "git commit -am \"Clean master not migrated to add future REAMDE.md\"";
                        execCommand(commandManager, workUnit.directory, gitCommand);
                    }

                    historyMgr.endStep(history, StatusEnum.DONE, null);

                    historyMgr.forceFlush();

                    markdownGenerator.generateSummaryReadme(historyMgr.
                        loadMigration(workUnit.migration.getId()), cleanedFilesManager, workUnit);

                    gitCommand = "git add README.md";
                    execCommand(commandManager, workUnit.directory, gitCommand);
                    gitCommand = "git commit -m \"📃 Add generated README.md\"";
                    execCommand(commandManager, workUnit.directory, gitCommand);
                    gitCommand = format("%s --set-upstream origin master", GIT_PUSH);
                    execCommand(commandManager, workUnit.directory, gitCommand);
                    historyMgr.endStep(history, StatusEnum.DONE, null);
                } catch (Exception exc) {
                    historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
                }

            } else {
                history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, format("%s, Tags, Branches", migration.getTrunk()));
                historyMgr.endStep(history, StatusEnum.IGNORED, "Skipping Migration : No Files Available. No Push to Gitlab");
            }

            // Finalize migration
            if (workUnit.warnings.get()) {
                migration.setStatus(StatusEnum.DONE_WITH_WARNINGS);
            } else {
                migration.setStatus(StatusEnum.DONE);
            }

            // migration was successful assure workingDirectory is set to empty so no reexecution is possible
            migration.setWorkingDirectory("");
            deleteWorkingRoot(workUnit, true);

            migrationRepository.save(migration);

            // Log all git config after operations
            logGitConfig(workUnit);

        } catch (Throwable exc) {
            history = migrationHistoryRepository.findFirstByMigration_IdOrderByIdDesc(migrationId);
            if (history != null) {
                LOG.error("Failed step : " + history.getStep(), exc);
                historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            }

            // A copy has been made and an error thrown. We can reexecute next time.
            if (commandManager.isReexecutable()) {
                migration.setWorkingDirectory(rootDir);
                deleteWorkingRoot(workUnit, false);

                LOG.info("Deleting working directory");
                LOG.info("REASON:commandManager.isReexecutable() AND ERROR during migration");
            }
            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
        } finally {
            LOG.debug("=====           Commands Executed          =======");
            LOG.debug("==================================================");
            commandManager.getCommandLog().forEach((k, v) -> LOG.debug("Directory : " + k + " Command : " + v));
            LOG.debug("==================================================");

            if (applicationProperties.getFlags().getCleanupWorkDirectory()) {
                // TODO : handle case where already deleted due to migration failure. See above.
                deleteWorkingRoot(workUnit, false);
            } else {
                LOG.info("Not cleaning up working directory");
                LOG.info("REASON:applicationProperties.getFlags().getCleanupWorkDirectory()==True");
            }
            LOG.info(format("Migration from SVN (%s) %s to Gitlab %s group completed with status %s",
                migration.getSvnGroup(), migration.getSvnProject(),
                migration.getGitlabGroup(),
                migration.getStatus()));
        }
        return new AsyncResult<>("THE_END");
    }

    /**
     * Delete working directory.
     * Can be in context of usual cleanup.
     * Can be in context of failed migration (in preparation for clean initialisation next time)
     *
     * @param workUnit
     * @param copy boolean indicating whether to delete the copy or not
     */
    private void deleteWorkingRoot(WorkUnit workUnit, boolean copy) {

        // Construct folder name (will be copy or not)
        String folderToDelete = format("%s%s", workUnit.root, (copy ? "_copy" : ""));

        // 7. Clean work directory
        MigrationHistory history = historyMgr.startStep(workUnit.migration,
            StepEnum.CLEANING, format("Remove %s", folderToDelete));

        // Sanity check
        if (!StringUtils.isBlank(folderToDelete) && folderToDelete.contains("20") && folderToDelete.contains("_")) {

            try {

                if (isWindows) {
                    // FileUtils.deleteDirectory(new File(folderToDelete));
                    // JBU : Fails occassionally on windows with file lock issue
                    // FileUtils.deleteDirectory(new File(folderToDelete));
                    // JBU : Fails on windows not able to delete a large number of files?..
                    String gitCommand = format("rd /s /q %s", folderToDelete);
                    execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand);

                } else {
                    // Seems to work ok on linux. Keeping Java command for the moment
                    FileSystemUtils.deleteRecursively(new File(folderToDelete));
                }

                historyMgr.endStep(history, StatusEnum.DONE, null);
            } catch (Exception exc) {
                LOG.error("Failed deleteDirectory: ", exc);
                historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            }

        } else {
            LOG.error("Failed deleteDirectory: Badly formed delete path");
            historyMgr.endStep(history, StatusEnum.FAILED, "Badly formed delete path");
        }
    }

    /**
     * When Git Svn Clone step is completed (and associated cleanup)
     * we copy the filesystem so that the migration can be reexecuted from this clean state.
     * This avoids waiting for lengthy Git svn clone step
     *
     * @param workUnit
     * @throws IOException
     * @throws InterruptedException
     */
    private void copyRootDirectory(WorkUnit workUnit) throws IOException, InterruptedException {

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_COPY_ROOT_FOLDER,
            (workUnit.commandManager.isFirstAttemptMigration() ? "" : Constants.REEXECUTION_SKIPPING) +
                "Copying Root Folder");

        if (workUnit.commandManager.isFirstAttemptMigration()) {

            String gitCommand = "";
            if (isWindows) {
                // /J Copy using unbuffered I/O. Recommended for very large files.
                gitCommand = format("Xcopy /E /I /H /Q %s %s_copy", workUnit.root, workUnit.root);
            } else {
                // cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
                // root has no trailling / e.g. folder_12345
                gitCommand = format("cp -a %s %s_copy", workUnit.root, workUnit.root);
            }
            execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand);

        }

        historyMgr.endStep(history, StatusEnum.DONE, null);
    }

    /**
     * In a migration reexectution the first step is to recuperate a clean state.
     *
     * @param workUnit
     * @throws IOException
     * @throws InterruptedException
     */
    private void initRootDirectoryFromCopy(WorkUnit workUnit) throws IOException, InterruptedException {


        if (!workUnit.commandManager.isFirstAttemptMigration()) {

            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_COPY_ROOT_FOLDER,
                (workUnit.commandManager.isFirstAttemptMigration() ? "" : Constants.REEXECUTION_SKIPPING) +
                    "Initialising Root Directory from Copy in context of migration reexecution.");

            // The clean copy folder is used to reinitialise the workUnit.root Folder
            String gitCommand = "";
            if (isWindows) {
                // /J Copy using unbuffered I/O. Recommended for very large files.
                gitCommand = format("Xcopy /E /I /H /Q %s_copy %s", workUnit.root, workUnit.root);
            } else {
                // cp -a /source/. /dest/ ("-a" is recursive "." means files and folders including hidden)
                // root has no trailling / e.g. folder_12345
                gitCommand = format("cp -a %s_copy %s", workUnit.root, workUnit.root);
            }
            execCommand(workUnit.commandManager, Shell.formatDirectory(applicationProperties.work.directory), gitCommand);

            // git reset incase a deployment has changed permissions
            // deployment of application seems to change files from 644 to 755 which is not desired.
            gitCommand = "git reset --hard HEAD";
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

            historyMgr.endStep(history, StatusEnum.DONE, null);
        }

    }


    private void logGitConfig(WorkUnit workUnit) throws IOException, InterruptedException {

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_SHOW_CONFIG, "Log Git Config and origin of config.");
        String gitCommand = "git config --list --show-origin";
        execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
        historyMgr.endStep(history, StatusEnum.DONE, null);
    }

    private void checkGitConfig(WorkUnit workUnit) throws IOException, InterruptedException {

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_SET_CONFIG, "Log Git Config and origin of config.");
        String gitCommand = "git config user.name";
        //String workDir = format("%s/%s", workUnit.directory, workUnit.migration.getSvnProject());
        try {
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
        } catch (RuntimeException rEx) {
            LOG.info("Git user.email and user.name not set, use default values based on gitlab user set in UI");
            gitCommand = format("git config user.email %s@svn2git.fake", workUnit.migration.getUser());
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
            gitCommand = format("git config user.name %s", workUnit.migration.getUser());
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
        } finally {
            historyMgr.endStep(history, StatusEnum.DONE, null);
        }
    }

    private void logUlimit(WorkUnit workUnit) throws IOException, InterruptedException {

        // On linux servers trace what ulimit value is
        if (!isWindows) {
            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.ULIMIT, "Show Ulimit -u value.");
            try {
                String command = "ulimit -u";
                execCommand(workUnit.commandManager, workUnit.directory, command);
            } catch (Exception exc) {
                // Ignore exception as it's just info displayed
            } finally {
                historyMgr.endStep(history, StatusEnum.DONE, null);
            }
        }

    }


    private void addDynamicLocalConfig(WorkUnit workUnit, String dynamicLocalConfig, String dynamicLocalConfigDesc) throws IOException, InterruptedException {

        if (StringUtils.isNotEmpty(dynamicLocalConfig) && StringUtils.isNotEmpty(dynamicLocalConfigDesc)) {

            String[] configParts = dynamicLocalConfig.split(" ");

            if (configParts.length == 2) {

                MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_DYNAMIC_LOCAL_CONFIG, dynamicLocalConfigDesc);

                LOG.info("Setting Git Config");
                // apply new local config
                String gitCommand = "git config " + dynamicLocalConfig;
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                //display value after
                LOG.info("Checking Git Config");
                gitCommand = "git config " + configParts[0];
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                historyMgr.endStep(history, StatusEnum.DONE, null);

            } else {
                LOG.warn("Problem applying dynamic git local configuration!!!");
            }

        } else {
            LOG.warn("Problem applying dynamic git local configuration!!!");
        }


    }

    /**
     * Create empty repository
     *
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String initDirectory(WorkUnit workUnit) throws IOException, InterruptedException {
        String svn = StringUtils.isEmpty(workUnit.migration.getSvnProject()) ?
            workUnit.migration.getSvnGroup()
            : workUnit.migration.getSvnProject();

        if (workUnit.commandManager.isFirstAttemptMigration()) {

            String mkdir;
            if (isWindows) {
                String path = workUnit.directory.startsWith("/") ? format("%s%s", System.getenv("SystemDrive"), workUnit.directory) : workUnit.directory;
                path = path.replaceAll("/", "\\\\");
                mkdir = format("mkdir %s", path);
            } else {
                mkdir = format("mkdir -p %s", workUnit.directory);
            }
            execCommand(workUnit.commandManager, applicationProperties.work.directory, mkdir);

        }

        return svn;
    }
}
