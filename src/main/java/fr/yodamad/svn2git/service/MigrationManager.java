package fr.yodamad.svn2git.service;

import com.madgag.git.bfg.cli.Main;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.*;

@Service
public class MigrationManager {

    public static final String REFS_REMOTES_ORIGIN_TAGS = "refs/remotes/origin/tags/";
    public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    @Value("${svn.url}") String svnUrl;
    @Value("${gitlab.url}") String gitlabUrl;
    @Value("${gitlab.svc-account}") String gitlabSvcUser;
    @Value("${gitlab.token}") String gitlabSvcToken;

    private static final Logger LOG = LoggerFactory.getLogger(MigrationManager.class);

    private final GitlabAdmin gitlab;
    private final MigrationRepository migrationRepository;
    private final MigrationHistoryRepository migrationHistoryRepository;

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public MigrationManager(final GitlabAdmin gitlabAdmin,
                            final MigrationRepository migrationRepository,
                            final MigrationHistoryRepository migrationHistoryRepository) {
        this.gitlab = gitlabAdmin;
        this.migrationRepository = migrationRepository;
        this.migrationHistoryRepository = migrationHistoryRepository;
    }

    @Async
    public void startMigration(final long migrationId) {
        Migration migration = migrationRepository.findById(migrationId).get();
        MigrationHistory history = null;
        String rootWorkingDir = workingDir(migration);
        String gitWorkingDir = gitWorkingDir(migration);

        try (
            Repository localRepo = new FileRepository(gitDir(gitWorkingDir));
            Git git = new Git(localRepo)) {

            // Start migration
            migration.setStatus(StatusEnum.RUNNING);
            migrationRepository.save(migration);

            // 1. Create project on gitlab : OK
            history = startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, gitlabUrl + migration.getGitlabGroup());

            Group group = gitlab.groupApi().getGroup(migration.getGitlabGroup());
            gitlab.projectApi().createProject(group.getId(), migration.getSvnProject());

            endStep(history, StatusEnum.DONE);

            // 2. Checkout SVN repository : OK

            history = startStep(migration, StepEnum.SVN_CHECKOUT, svnUrl + migration.getSvnGroup());

            String mkdir = "mkdir " + gitWorkingDir;
            execCommand(System.getProperty(JAVA_IO_TMPDIR), mkdir);

            // 2.1. Clone as mirror empty repository, required for BFG
            String initCommand = format("git clone %s/%s/%s.git %s",
                gitlabUrl,
                migration.getGitlabGroup(),
                migration.getSvnProject(),
                migration.getGitlabGroup());
            execCommand(rootWorkingDir, initCommand);

            // 2.2. SVN checkout
            String cloneCommand = format("git svn clone --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s%s",
                migration.getSvnProject(),
                migration.getSvnProject(),
                migration.getSvnProject(),
                svnUrl,
                migration.getSvnGroup());
            execCommand(rootWorkingDir, cloneCommand);

            endStep(history, StatusEnum.DONE);

            // 3. Clean large files
            history = startStep(migration, StepEnum.GIT_CLEANING, "*.zip");

            String gitCommand = "git gc";
            execCommand(gitWorkingDir, gitCommand);

            Main.main(new String[]{"--strip-blobs-bigger-than", "1M", "--no-blob-protection", gitWorkingDir});

            gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive";
            execCommand(gitWorkingDir, gitCommand);

            endStep(history, StatusEnum.DONE);

            // 4. Git push master based on SVN trunk
            history = startStep(migration, StepEnum.GIT_PUSH, "trunk -> master");

            String gitUrl = format("%s/%s/%s.git",
                gitlabUrl,
                migration.getGitlabGroup(),
                migration.getSvnProject());
            addRemote(git,"origin", gitUrl);

            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(gitlabSvcUser, gitlabSvcToken));
            pushCommand.call();

            endStep(history, StatusEnum.DONE);

            // 5. List branches & tags
            List<Ref> svnBranches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            // Extract branches
            List<String> gitBranches = svnBranches.stream()
                // Remove tags
                .filter(b -> !b.getName().startsWith(REFS_REMOTES_ORIGIN_TAGS))
                // Remove master/trunk
                .filter(b -> !b.getName().contains("master"))
                .filter(b -> !b.getName().contains("trunk"))
                .map(Ref::getName).collect(Collectors.toList());
            // Extract tags
            List<String> gitTags = svnBranches.stream()
                // Only tags
                .filter(b -> b.getName().startsWith(REFS_REMOTES_ORIGIN_TAGS))
                // Remove temp tags
                .filter(b -> !b.getName().contains("@"))
                .map(Ref::getName).collect(Collectors.toList());

            gitBranches.forEach(b -> pushBranch(migration, b));
            gitTags.forEach(t -> pushTag(migration, t));

            migration.setStatus(StatusEnum.DONE);
            migrationRepository.save(migration);
        } catch (Exception exc) {
            if (history != null) {
                LOG.error("Failed step : " + history.getStep(), exc);
                endStep(history, StatusEnum.FAILED);
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
        if (isWindows) {
            return System.getProperty(JAVA_IO_TMPDIR) + "\\" + mig.getId();
        }
        return System.getProperty(JAVA_IO_TMPDIR) + "/" + mig.getId();
    }

    /**
     * Get git working directory
     * @param migration
     * @return
     */
    private static String gitWorkingDir(Migration migration) {
        if (isWindows) {
            return workingDir(migration) + "\\" + migration.getSvnGroup();
        }
        return workingDir(migration) + "/" + migration.getSvnGroup();
    }

    /**
     * Get git directory
     * @param workingDir
     * @return
     */
    private static String gitDir(String workingDir) {
        if (isWindows) {
            return workingDir + "\\.git";
        }
        return workingDir + "/.git";
    }

    /**
     * Execute a commmand through process
     * @param directory Directory in which running command
     * @param command command to execute
     * @throws InterruptedException
     * @throws IOException
     */
    private static void execCommand(String directory, String command) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.directory(new File(directory));

        LOG.debug(format("Exec command : %s", command));
        LOG.debug(format("in %s", directory));

        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        int exitCode = process.waitFor();
        LOG.debug(format("Exit : %d", exitCode));

        assert exitCode == 0;
    }

    // Tasks
    /**
     * Add remote url to git config
     * @param git Git instance
     * @param remoteName Remote name for repository
     * @param remoteUrl Remote URL
     * @throws IOException
     * @throws URISyntaxException
     * @throws GitAPIException
     */
    private static void addRemote(Git git, String remoteName, String remoteUrl) throws IOException, URISyntaxException, GitAPIException {
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName(remoteName);
        remoteAddCommand.setUri(new URIish(remoteUrl));
        remoteAddCommand.call();
    }

    /**
     * Push a branch
     * @param migration Migration object
     * @param branch Branch to migrate
     */
    private void pushBranch(Migration migration, String branch) {
        MigrationHistory history = startStep(migration, StepEnum.GIT_PUSH, branch);
        try {
            String branchName = branch.replaceFirst("refs/remotes/origin/", "");
            LOG.debug(format("Branch %s", branchName));

            String gitCommand = format("git checkout -b %s %s", branchName, branch);
            execCommand(gitWorkingDir(migration), gitCommand);

            gitCommand = format("git push -u origin %s", branchName);
            execCommand(gitWorkingDir(migration), gitCommand);

            endStep(history, StatusEnum.DONE);
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to push branch", gitEx);
            endStep(history, StatusEnum.FAILED);
        }
    }

    /**
     * Push a tag
     * @param migration Migration object
     * @param tag Tag to migrate
     */
    private void pushTag(Migration migration, String tag) {
        MigrationHistory history = startStep(migration, StepEnum.GIT_PUSH, tag);
        try {
            String tagName = tag.replaceFirst(REFS_REMOTES_ORIGIN_TAGS, "");
            LOG.debug(">>>>>>>>>>> Tag " + tagName);

            String gitCommand = format("git checkout -b tmp_tag %s", tag);
            execCommand(gitWorkingDir(migration), gitCommand);

            gitCommand = "git checkout master";
            execCommand(gitWorkingDir(migration), gitCommand);

            gitCommand = format("git tag %s tmp_tag", tagName);
            execCommand(gitWorkingDir(migration), gitCommand);

            gitCommand = format("git push -u origin %s", tagName);
            execCommand(gitWorkingDir(migration), gitCommand);

            gitCommand = "git branch -D tmp_tag";
            execCommand(gitWorkingDir(migration), gitCommand);

            endStep(history, StatusEnum.DONE);
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to push branch", gitEx);
            endStep(history, StatusEnum.FAILED);
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
    private void endStep(MigrationHistory history, StatusEnum status) {
        history.setStatus(status);
        migrationHistoryRepository.save(history);
    }


    // Utils
    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
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
