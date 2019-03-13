package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

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

    /** Gitlab API. */

    // Managers
    private final HistoryManager historyMgr;
    private final GitManager gitManager;
    private final Cleaner cleaner;
    // Repositories
    private final MigrationRepository migrationRepository;
    private final MigrationHistoryRepository migrationHistoryRepository;
    // Configuration
    private final ApplicationProperties applicationProperties;

    public MigrationManager(final Cleaner cleaner,
                            final GitManager gitManager,
                            final HistoryManager historyManager,
                            final MigrationRepository migrationRepository,
                            final MigrationHistoryRepository migrationHistoryRepository,
                            final ApplicationProperties applicationProperties) {
        this.cleaner = cleaner;
        this.gitManager = gitManager;
        this.historyMgr = historyManager;
        this.migrationRepository = migrationRepository;
        this.migrationHistoryRepository = migrationHistoryRepository;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Start a migration in a dedicated thread
     * @param migrationId ID for migration to start
     * @param retry Flag to know if it's the first attempt or a retry
     */
    @Async
    public void startMigration(final long migrationId, final boolean retry) {
        String gitCommand;
        Migration migration = migrationRepository.findById(migrationId).get();
        MigrationHistory history = null;
        String rootDir;

        try {
            history = historyMgr.startStep(migration, StepEnum.INIT, "Create working directory");
            rootDir = workingDir(applicationProperties.work.directory, migration);
            historyMgr.endStep(history, StatusEnum.DONE,null);
        } catch (IOException | InterruptedException ex) {
            historyMgr.endStep(history, StatusEnum.FAILED, format("Failed to create directory : %s", ex.getMessage()));
            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
            return;
        }

        WorkUnit workUnit = new WorkUnit(migration, rootDir, gitWorkingDir(rootDir, migration.getSvnGroup()), new AtomicBoolean(false));

        try {

            // in retry case, clean gitlab
            if (retry) {
                // TODO : Remove gitlab group
                LOG.info("This a retry, clean gitlab");
            }

            // Start migration
            migration.setStatus(StatusEnum.RUNNING);
            migrationRepository.save(migration);

            // 1. Create project on gitlab : OK
            gitManager.createGitlabProject(migration);

            // 2. Checkout empty repository : OK
            String svn = initDirectory(workUnit);

            // 2.2. SVN checkout
            gitManager.gitSvnClone(workUnit);

            // 2.3. Remove phantom branches
            history = historyMgr.startStep(migration, StepEnum.BRANCH_CLEAN, "Clean removed SVN branches");
            AtomicBoolean withWarning = new AtomicBoolean(false);
            final List<String> failedBranches = new ArrayList<>();

            // List git branch
            String gitBranchList = "git branch -r | sed \"s|^[[:space:]]*||\" | grep -v '^tags/' | sed -e \"s/origin\\///g\" > git-branch-list";
            execCommand(workUnit.directory, gitBranchList);
            // List svn branch
            String svnBranchList = format("svn ls %s%s/%s/branches | sed \"s|^[[:space:]]*||\" | sed \"s|/$||\" > svn-branch-list",
                workUnit.migration.getSvnUrl(), workUnit.migration.getSvnGroup(), workUnit.migration.getSvnProject());
            execCommand(workUnit.directory, svnBranchList);
            // Diff git & svn branches
            String diff = "diff -u git-branch-list svn-branch-list | grep \"^-\" | sed \"s|^-||\" | grep -v \"^trunk$\" | grep -v \"^--\" > old-branch-list";
            execCommand(workUnit.directory, diff);

            // Remove none git branches
            Path oldBranchFile = Paths.get(workUnit.directory, "old-branch-list");
            try (Stream<String> lines = Files.lines(oldBranchFile)) {
                    lines.forEach(line -> {
                        try {
                            String cleanCmd = format("git branch -d -r origin/%s", line);
                            execCommand(workUnit.directory, cleanCmd);
                            cleanCmd = format("rm -rf .git/svn/refs/remotes/origin/%s", line);
                            execCommand(workUnit.directory, cleanCmd);
                        } catch (IOException | InterruptedException ex) {
                            LOG.error("Cannot remove branch " + line);
                            withWarning.set(true);
                            failedBranches.add(line);
                        }
                    });
            }
            if (withWarning.get()) {
                //  Some branches have failed
                historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, format("Failed to remove branches %s", failedBranches));
            } else {
                historyMgr.endStep(history, StatusEnum.DONE, null);
            }


            // 3. Clean files
            boolean cleanExtensions = cleaner.cleanForbiddenExtensions(workUnit);
            boolean cleanLargeFiles = cleaner.cleanLargeFiles(workUnit);

            if (cleanExtensions || cleanLargeFiles) {
                gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive";
                execCommand(workUnit.directory, gitCommand);
                gitCommand = "git reset HEAD";
                execCommand(workUnit.directory, gitCommand);
                gitCommand = "git clean -fd";
                execCommand(workUnit.directory, gitCommand);
            }

            // 4. Git push master based on SVN trunk
            if (migration.getTrunk() != null && migration.getTrunk().equals("*")) {
                history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "SVN trunk -> GitLab master");

                // Set origin
                execCommand(workUnit.directory,
                    gitManager.buildRemoteCommand(workUnit, svn, false),
                    gitManager.buildRemoteCommand(workUnit, svn, true));

                // if no history option set
                if (migration.getSvnHistory().equals("nothing")) {
                    gitManager.removeHistory(workUnit, MASTER, history);
                } else {
                    // Push with upstream
                    gitCommand = format("%s --set-upstream origin master", GIT_PUSH);
                    execCommand(workUnit.directory, gitCommand);
                    historyMgr.endStep(history, StatusEnum.DONE, null);
                }

                // Clean pending file(s) removed by BFG
                gitCommand = "git reset --hard origin/master";
                execCommand(workUnit.directory, gitCommand);

                // 5. Apply mappings if some
                boolean warning = gitManager.applyMapping(workUnit, MASTER);
                workUnit.warnings.set(workUnit.warnings.get() || warning);
            } else {
                history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "Trunk");
                historyMgr.endStep(history, StatusEnum.IGNORED, "Skip trunk");
            }

            // 6. List branches & tags
            List<String> remotes = GitManager.branchList(workUnit.directory);
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

            if (workUnit.warnings.get()) {
                migration.setStatus(StatusEnum.DONE_WITH_WARNINGS);
            } else{
                migration.setStatus(StatusEnum.DONE);
            }
            migrationRepository.save(migration);
        } catch (Throwable exc) {
            history = migrationHistoryRepository.findFirstByMigration_IdOrderByIdDesc(migrationId);
            if (history != null) {
                LOG.error("Failed step : " + history.getStep(), exc);
                historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            }

            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
        } finally {
            // 7. Clean work directory
            history = historyMgr.startStep(migration, StepEnum.CLEANING, format("Remove %s", workUnit.root));
            try {
                FileUtils.deleteDirectory(new File(workUnit.root));
                historyMgr.endStep(history, StatusEnum.DONE, null);
            } catch (Exception exc) {
                historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            }
        }
    }

    /**
     * Create empty repository
     * @param workUnit
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String initDirectory(WorkUnit workUnit) throws IOException, InterruptedException {
        String svn = StringUtils.isEmpty(workUnit.migration.getSvnProject()) ?
            workUnit.migration.getSvnGroup()
            : workUnit.migration.getSvnProject();

        String mkdir;
        if (isWindows) {
            mkdir = format("mkdir %s", workUnit.directory);
        } else {
            mkdir = format("mkdir -p %s", workUnit.directory);
        }
        execCommand(applicationProperties.work.directory, mkdir);
        return svn;
    }
}
