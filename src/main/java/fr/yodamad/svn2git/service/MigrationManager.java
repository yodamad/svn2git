package fr.yodamad.svn2git.service;

import com.madgag.git.bfg.cli.Main;
import fr.yodamad.svn2git.domain.Mapping;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MappingRepository;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.models.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
public class MigrationManager {

    /** Default ref origin for tags. */
    private static final String ORIGIN_TAGS = "origin/tags/";
    /** Temp directory. */
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    /** Default branch. */
    private static final String MASTER = "master";
    /** Git push command. */
    private static final String GIT_PUSH = "git push";
    /** Stars to hide sensitive data. */
    private static final String STARS = "******";

    // Configuration
    @Value("${gitlab.url}") String gitlabUrl;
    @Value("${gitlab.svc-account}") String gitlabSvcUser;

    private static final Logger LOG = LoggerFactory.getLogger(MigrationManager.class);

    /** Gitlab API. */
    private GitlabAdmin gitlab;
    // Repositories
    private final MigrationRepository migrationRepository;
    private final MigrationHistoryRepository migrationHistoryRepository;
    private final MappingRepository mappingRepository;

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public MigrationManager(final GitlabAdmin gitlabAdmin,
                            final MigrationRepository migrationRepository,
                            final MigrationHistoryRepository migrationHistoryRepository,
                            final MappingRepository mappingRepository) {
        this.gitlab = gitlabAdmin;
        this.migrationRepository = migrationRepository;
        this.migrationHistoryRepository = migrationHistoryRepository;
        this.mappingRepository = mappingRepository;
    }

    /**
     * Start a migration in a dedicated thread
     * @param migrationId ID for migration to start
     */
    @Async
    public void startMigration(final long migrationId) {
        Migration migration = migrationRepository.findById(migrationId).get();
        MigrationHistory history = null;
        String rootWorkingDir = workingDir(migration);
        String gitWorkingDir = gitWorkingDir(migration);

        try {

            // Start migration
            migration.setStatus(StatusEnum.RUNNING);
            migrationRepository.save(migration);

            // 1. Create project on gitlab : OK
            history = startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, migration.getGitlabUrl() + migration.getGitlabGroup());

            GitlabAdmin gitlabAdmin = gitlab;
            if (!gitlabUrl.equalsIgnoreCase(migration.getGitlabUrl())) {
                gitlabAdmin = new GitlabAdmin(migration.getGitlabUrl(), migration.getGitlabToken());
            }
            Group group = gitlabAdmin.groupApi().getGroup(migration.getGitlabGroup());
            gitlabAdmin.projectApi().createProject(group.getId(), migration.getSvnProject());

            endStep(history, StatusEnum.DONE, null);

            // 2. Checkout SVN repository : OK
            String initCommand = format("git clone %s/%s/%s.git %s",
                migration.getGitlabUrl(),
                migration.getGitlabGroup(),
                migration.getSvnProject(),
                migration.getGitlabGroup());

            history = startStep(migration, StepEnum.GIT_CLONE, initCommand);

            String mkdir = "mkdir " + gitWorkingDir;
            execCommand(System.getProperty(JAVA_IO_TMPDIR), mkdir);

            // 2.1. Clone as mirror empty repository, required for BFG
            execCommand(rootWorkingDir, initCommand);

            endStep(history, StatusEnum.DONE, null);

            // 2.2. SVN checkout
            String cloneCommand;
            String safeCommand;
            if (StringUtils.isEmpty(migration.getSvnUser())) {
                cloneCommand = format("git svn clone --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                    migration.getSvnProject(),
                    migration.getSvnProject(),
                    migration.getSvnProject(),
                    migration.getSvnUrl(),
                    migration.getSvnGroup());
                safeCommand = cloneCommand;
            } else {
                String escapedPassword = StringEscapeUtils.escapeJava(migration.getSvnPassword());
                cloneCommand = format("echo %s | git svn clone --username %s --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                    escapedPassword,
                    migration.getSvnUser(),
                    migration.getSvnProject(),
                    migration.getSvnProject(),
                    migration.getSvnProject(),
                    migration.getSvnUrl(),
                    migration.getSvnGroup());
                safeCommand = format("echo %s | git svn clone --username %s --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                    STARS,
                    migration.getSvnUser(),
                    migration.getSvnProject(),
                    migration.getSvnProject(),
                    migration.getSvnProject(),
                    migration.getSvnUrl(),
                    migration.getSvnGroup());
            }

            history = startStep(migration, StepEnum.SVN_CHECKOUT, safeCommand);
            execCommand(rootWorkingDir, cloneCommand, safeCommand);

            endStep(history, StatusEnum.DONE, null);

            // 3. Clean files
            boolean clean = false;
            String gitCommand;

            if (!StringUtils.isEmpty(migration.getForbiddenFileExtensions())) {
                // 3.1 Clean files based on their extensions
                Arrays.stream(migration.getForbiddenFileExtensions().split(","))
                    .forEach(s -> {
                        MigrationHistory innerHistory = startStep(migration, StepEnum.GIT_CLEANING, format("Remove files with extension : %s", s));
                        try {
                            Main.main(new String[]{"--delete-files", s, "--no-blob-protection", gitWorkingDir});
                            endStep(innerHistory, StatusEnum.DONE, null);
                        } catch (Exception exc) {
                            endStep(innerHistory, StatusEnum.FAILED, exc.getMessage());
                        }
                    });
                clean = true;
            }

            if (!StringUtils.isEmpty(migration.getMaxFileSize()) && Character.isDigit(migration.getMaxFileSize().charAt(0))) {
                // 3.2 Clean files based on size
                history = startStep(migration, StepEnum.GIT_CLEANING, format("Remove files bigger than %s", migration.getMaxFileSize()));

                gitCommand = "git gc";
                execCommand(gitWorkingDir, gitCommand);

                Main.main(new String[]{"--strip-blobs-bigger-than", migration.getMaxFileSize(), "--no-blob-protection", gitWorkingDir});
                clean = true;
                endStep(history, StatusEnum.DONE, null);
            }

            if (clean) {
                gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive";
                execCommand(gitWorkingDir, gitCommand);
            }

            // 4. Git push master based on SVN trunk
            history = startStep(migration, StepEnum.GIT_PUSH, "SVN trunk -> GitLab master");

            execCommand(gitWorkingDir, GIT_PUSH);

            endStep(history, StatusEnum.DONE, null);

            // Clean pending file(s) removed by BFG
            gitCommand = "git reset --hard origin/master";
            execCommand(gitWorkingDir, gitCommand);

            // 5. Apply mappings if some
            applyMapping(gitWorkingDir, migration, MASTER);

            // 6. List branches & tags
            List<String> remotes = svnList(gitWorkingDir);
            // Extract branches
            List<String> gitBranches = remotes.stream()
                .map(String::trim)
                // Remove tags
                .filter(b -> !b.startsWith(ORIGIN_TAGS))
                // Remove master/trunk
                .filter(b -> !b.contains(MASTER))
                .filter(b -> !b.contains("trunk"))
                .collect(Collectors.toList());
            // Extract tags
            List<String> gitTags = remotes.stream()
                .map(String::trim)
                // Only tags
                .filter(b -> b.startsWith(ORIGIN_TAGS))
                // Remove temp tags
                .filter(b -> !b.contains("@"))
                .collect(Collectors.toList());

            gitBranches.forEach(b -> pushBranch(gitWorkingDir, migration, b));
            gitTags.forEach(t -> pushTag(gitWorkingDir, migration, t));

            // 7. Clean work directory
            history = startStep(migration, StepEnum.CLEANING, format("Remove %s", workingDir(migration)));
            try {
                FileUtils.deleteDirectory(new File(workingDir(migration)));
                endStep(history, StatusEnum.DONE, null);
            } catch (Exception exc) {
                endStep(history, StatusEnum.FAILED, exc.getMessage());
            }

            migration.setStatus(StatusEnum.DONE);
            migrationRepository.save(migration);
        } catch (Exception exc) {
            if (history != null) {
                LOG.error("Failed step : " + history.getStep(), exc);
                endStep(history, StatusEnum.FAILED, exc.getMessage());
            }

            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
        }
    }

    /**
     * Get working directory
     * @param mig
     * @return
     */
    private static String workingDir(Migration mig) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (isWindows) {
            return System.getProperty(JAVA_IO_TMPDIR) + "\\" + today + "_" + mig.getId();
        }
        return System.getProperty(JAVA_IO_TMPDIR) + "/" + today + "_" + mig.getId();
    }

    /**
     * Get git working directory
     * @param migration Migration to process
     * @return
     */
    private static String gitWorkingDir(Migration migration) {
        if (isWindows) {
            return workingDir(migration) + "\\" + migration.getSvnGroup();
        }
        return workingDir(migration) + "/" + migration.getSvnGroup();
    }

    /**
     * Execute a commmand through process
     * @param directory Directory in which running command
     * @param command command to execute
     * @throws InterruptedException
     * @throws IOException
     */
    private static int execCommand(String directory, String command) throws InterruptedException, IOException {
        return execCommand(directory, command, command);
    }

    /**
     * Execute a commmand through process without an alternative command to print in history
     * @param directory Directory in which running command
     * @param command command to execute
     * @param securedCommandToPrint command to print in history without password/token/...
     * @throws InterruptedException
     * @throws IOException
     */
    private static int execCommand(String directory, String command, String securedCommandToPrint) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.directory(new File(directory));

        LOG.debug(format("Exec command : %s", securedCommandToPrint));
        LOG.debug(format("in %s", directory));

        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        int exitCode = process.waitFor();
        LOG.debug(format("Exit : %d", exitCode));

        assert exitCode == 0;

        return exitCode;
    }

    // Tasks
    /**
     * Apply mappings configured
     * @param gitWorkingDir Current working directory
     * @param migration Migration in progress
     * @param branch Branch to process
     */
    private void applyMapping(String gitWorkingDir, Migration migration, String branch) {
        List<Mapping> mappings = mappingRepository.findAllByMigration(migration.getId());
        boolean workDone = false;
        if (!CollectionUtils.isEmpty(mappings)) {
            List<Boolean> results = mappings.stream()
                .map(mapping -> mvDirectory(gitWorkingDir, migration, mapping))
                .collect(Collectors.toList());
            workDone = results.contains(true);
        }

        if (workDone) {
            MigrationHistory history = startStep(migration, StepEnum.GIT_PUSH, format("Push moved elements on %s", branch));
            try {
                // git commit
                String gitCommand = "git add .";
                execCommand(gitWorkingDir, gitCommand);
                gitCommand = format("git commit -m \"Apply mappings on %s\"", branch);
                execCommand(gitWorkingDir, gitCommand);
                // git push
                execCommand(gitWorkingDir, GIT_PUSH);

                endStep(history, StatusEnum.DONE, null);
            } catch (IOException | InterruptedException iEx) {
                endStep(history, StatusEnum.FAILED, iEx.getMessage());
            }
        }
    }

    /**
     * Apply git mv
     * @param gitWorkingDir Working directory
     * @param migration Current migration
     * @param mapping Mapping to apply
     */
    private boolean mvDirectory(String gitWorkingDir, Migration migration, Mapping mapping) {
        MigrationHistory history;
        try {
            boolean workDone;
            if (mapping.getGitDirectory().equals("/") || mapping.getGitDirectory().equals(".")) {
                // For root directory, we need to loop for subdirectory
                List<Boolean> results = Files.list(Paths.get(gitWorkingDir, mapping.getSvnDirectory()))
                    .map(d -> mv(gitWorkingDir, migration, format("%s/%s", mapping.getSvnDirectory(), d.getFileName().toString()), d.getFileName().toString()))
                    .collect(Collectors.toList());
                workDone =  results.contains(true);
                if (results.isEmpty()) {
                    history = startStep(migration, StepEnum.GIT_MV, format("git mv %s %s", mapping.getSvnDirectory(), mapping.getGitDirectory()));
                    endStep(history, StatusEnum.IGNORED, null);
                }
            } else {
                workDone = mv(gitWorkingDir, migration, mapping.getSvnDirectory(), mapping.getGitDirectory());
            }
            return workDone;
        } catch (IOException gitEx) {
            LOG.error("Failed to mv directory", gitEx);
            return false;
        }
    }

    /**
     * Apply git mv
     * @param gitWorkingDir Current working directory
     * @param migration Migration in progress
     * @param svnDir Origin SVN element
     * @param gitDir Target Git element
     */
    private boolean mv(String gitWorkingDir, Migration migration, String svnDir, String gitDir) {
        MigrationHistory history = null;
        try {
            String gitCommand = format("git mv %s %s", svnDir, gitDir);
            history = startStep(migration, StepEnum.GIT_MV, gitCommand);
            // git mv
            int exitCode = execCommand(gitWorkingDir, gitCommand);

            if (128 == exitCode) {
                endStep(history, StatusEnum.IGNORED, null);
                return false;
            } else {
                endStep(history, StatusEnum.DONE, null);
                return true;
            }
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to mv directory", gitEx);
            endStep(history, StatusEnum.FAILED, gitEx.getMessage());
            return false;
        }
    }

    /**
     * List SVN branches & tags cloned
     * @param directory working directory
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private List<String> svnList(String directory) throws InterruptedException, IOException {
        String command = "git branch -r";
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        builder.directory(new File(directory));

        Process p = builder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<String> remotes = new ArrayList<>();
        reader.lines().iterator().forEachRemaining(remotes::add);

        p.waitFor();
        p.destroy();

        return remotes;
    }

    /**
     * Push a branch
     * @param gitWorkingDir Working directory
     * @param migration Migration object
     * @param branch Branch to migrate
     */
    private void pushBranch(String gitWorkingDir, Migration migration, String branch) throws RuntimeException {
        try {
            String branchName = branch.replaceFirst("refs/remotes/origin/", "");
            branchName = branchName.replaceFirst("origin/", "");
            LOG.debug(format("Branch %s", branchName));

            String gitCommand = format("git checkout -b %s %s", branchName, branch);
            execCommand(gitWorkingDir, gitCommand);
            execCommand(gitWorkingDir, GIT_PUSH);

            applyMapping(gitWorkingDir, migration, branch);
        } catch (IOException | InterruptedException iEx) {
            throw new RuntimeException();
        }
    }

    /**
     * Push a tag
     * @param gitWorkingDir Current working directory
     * @param migration Migration object
     * @param tag Tag to migrate
     */
    private void pushTag(String gitWorkingDir, Migration migration, String tag) {
        MigrationHistory history = startStep(migration, StepEnum.GIT_PUSH, tag);
        try {
            String tagName = tag.replaceFirst(ORIGIN_TAGS, "");
            LOG.debug(format("Tag %s", tagName));

            String gitCommand = format("git checkout -b tmp_tag %s", tag);
            execCommand(gitWorkingDir, gitCommand);

            gitCommand = "git checkout master";
            execCommand(gitWorkingDir, gitCommand);

            gitCommand = format("git tag %s tmp_tag", tagName);

            execCommand(gitWorkingDir, gitCommand);

            gitCommand = format("git push -u origin %s", tagName);
            execCommand(gitWorkingDir, gitCommand);

            gitCommand = "git branch -D tmp_tag";
            execCommand(gitWorkingDir, gitCommand);

            endStep(history, StatusEnum.DONE, null);
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to push branch", gitEx);
            endStep(history, StatusEnum.FAILED, gitEx.getMessage());
        }
    }

    // History management
    /**
     * Create a new history for migration
     * @param migration
     * @param step
     * @param data
     * @return
     */
    private MigrationHistory startStep(Migration migration, StepEnum step, String data) {
        MigrationHistory history = new MigrationHistory()
            .step(step)
            .migration(migration)
            .date(Instant.now())
            .status(StatusEnum.RUNNING);

        if (data != null) {
            history.data(data);
        }

        return migrationHistoryRepository.save(history);
    }

    /**
     * Update history
     * @param history
     */
    private void endStep(MigrationHistory history, StatusEnum status, String data) {
        history.setStatus(status);
        if (data != null) history.setData(data);
        migrationHistoryRepository.save(history);
    }


    // Utils
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                .forEach(consumer);
        }
    }
}
