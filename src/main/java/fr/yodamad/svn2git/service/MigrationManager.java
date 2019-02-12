package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private GitlabAdmin gitlab;
    // Managers
    private final HistoryManager historyMgr;
    private final GitManager gitManager;
    private final Cleaner cleaner;
    // Repositories
    private final MigrationRepository migrationRepository;
    // Configuration
    private final ApplicationProperties applicationProperties;

    public MigrationManager(final Cleaner cleaner,
                            final GitlabAdmin gitlabAdmin,
                            final GitManager gitManager,
                            final HistoryManager historyManager,
                            final MigrationRepository migrationRepository,
                            final ApplicationProperties applicationProperties) {
        this.cleaner = cleaner;
        this.gitlab = gitlabAdmin;
        this.gitManager = gitManager;
        this.historyMgr = historyManager;
        this.migrationRepository = migrationRepository;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Start a migration in a dedicated thread
     * @param migrationId ID for migration to start
     */
    @Async
    public void startMigration(final long migrationId) {
        String gitCommand;
        Migration migration = migrationRepository.findById(migrationId).get();
        MigrationHistory history = null;

        try {

            String rootDir = workingDir(applicationProperties.work.directory, migration);
            WorkUnit workUnit = new WorkUnit(migration, rootDir, gitWorkingDir(rootDir, migration.getSvnGroup()), new AtomicBoolean(false));

            // Start migration
            migration.setStatus(StatusEnum.RUNNING);
            migrationRepository.save(migration);

            // 1. Create project on gitlab : OK
            createGitlabProject(migration);

            // 2. Checkout empty repository : OK
            String svn = gitManager.gitClone(workUnit);

            // 2.2. SVN checkout
            gitManager.gitSvnClone(workUnit);

            // 3. Clean files
            boolean cleanExtensions = cleaner.cleanForbiddenExtensions(workUnit);
            boolean cleanLargeFiles = cleaner.cleanLargeFiles(workUnit);

            if (cleanExtensions || cleanLargeFiles) {
                gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive";
                execCommand(workUnit.directory, gitCommand);
            }

            // 4. Git push master based on SVN trunk
            if (migration.getTrunk().equals("*")) {
                history = historyMgr.startStep(migration, StepEnum.GIT_PUSH, "SVN trunk -> GitLab master");

                if (StringUtils.isEmpty(migration.getSvnProject())) {
                    // Set origin
                    String remoteCommand = format("git remote add origin %s/%s/%s.git",
                        migration.getGitlabUrl(),
                        migration.getGitlabGroup(),
                        svn);
                    execCommand(workUnit.directory, remoteCommand);
                }

                // if no history option set
                if (migration.getSvnHistory().equals("nothing")) {
                    gitManager.removeHistory(workUnit, MASTER);
                } else {
                    // if using root, additional step
                    if (StringUtils.isEmpty(migration.getSvnProject())) {


                        // Push with upstream
                        gitCommand = format("%s --set-upstream origin master", GIT_PUSH);
                        execCommand(workUnit.directory, gitCommand);
                    } else {
                        execCommand(workUnit.directory, GIT_PUSH);
                    }
                }
                historyMgr.endStep(history, StatusEnum.DONE, null);

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

            // 7. Clean work directory
            history = historyMgr.startStep(migration, StepEnum.CLEANING, format("Remove %s", workUnit.root));
            try {
                FileUtils.deleteDirectory(new File(workUnit.root));
                historyMgr.endStep(history, StatusEnum.DONE, null);
            } catch (Exception exc) {
                historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
                workUnit.warnings.set(true);
            }

            if (workUnit.warnings.get()) {
                migration.setStatus(StatusEnum.DONE_WITH_WARNINGS);
            } else{
                migration.setStatus(StatusEnum.DONE);
            }
            migrationRepository.save(migration);
        } catch (Exception exc) {
            if (history != null) {
                LOG.error("Failed step : " + history.getStep(), exc);
                historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            }

            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
        }
    }

    /**
     * Create project in GitLab
     * @param migration
     * @throws GitLabApiException
     */
    private void createGitlabProject(Migration migration) throws GitLabApiException {
        MigrationHistory history = historyMgr.startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, migration.getGitlabUrl() + migration.getGitlabGroup());

        GitlabAdmin gitlabAdmin = gitlab;
        if (!applicationProperties.gitlab.url.equalsIgnoreCase(migration.getGitlabUrl())) {
            GitLabApi api = new GitLabApi(migration.getGitlabUrl(), migration.getGitlabToken());
            gitlabAdmin.setGitLabApi(api);
        }
        try {
            Group group = gitlabAdmin.groupApi().getGroup(migration.getGitlabGroup());

            // If no svn project specified, use svn group instead
            if (StringUtils.isEmpty(migration.getSvnProject())) {
                gitlabAdmin.projectApi().createProject(group.getId(), migration.getSvnGroup());
            } else {
                // split svn structure to create gitlab elements (group(s), project)
                String[] structure = migration.getSvnProject().split("/");
                Integer groupId = group.getId();
                if (structure.length > 2) {
                    for (int module = 1; module < structure.length - 2; module++) {
                        Group gitlabSubGroup = new Group();
                        gitlabSubGroup.setName(structure[module]);
                        gitlabSubGroup.setPath(structure[module]);
                        gitlabSubGroup.setParentId(groupId);
                        groupId = gitlabAdmin.groupApi().addGroup(gitlabSubGroup).getId();
                    }
                }
                gitlabAdmin.projectApi().createProject(groupId, structure[structure.length - 1]);
            }
            historyMgr.endStep(history, StatusEnum.DONE, null);
        } catch (GitLabApiException exc) {
            historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            throw exc;
        }
    }
}
