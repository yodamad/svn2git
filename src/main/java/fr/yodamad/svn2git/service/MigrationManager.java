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
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class MigrationManager {

    @Value("${svn.url}") String svnUrl;
    @Value("${gitlab.url}") String gitlabUrl;

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
            Repository localRepo = new FileRepository(gitWorkingDir);
            Git git = new Git(localRepo)) {

            // Start migration
            migration.setStatus(StatusEnum.RUNNING);
            migrationRepository.save(migration);

            // 1. Create project on gitlab : OK
            history = startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, gitlabUrl + migration.getGitlabGroup());

            Group group = gitlab.groupApi().getGroup(migration.getGitlabGroup());
            Project project = gitlab.projectApi().createProject(group.getId(), migration.getSvnProject());

            endStep(history);

            // 1. Checkout SVN repository : OK
            history = startStep(migration, StepEnum.SVN_CHECKOUT, svnUrl + migration.getSvnGroup());

            String mkdir = "mkdir " + migration.getId();
            execCommand(System.getProperty("java.io.tmpdir"), mkdir);

            // 1.
            String initCommand = String.format("git clone --mirror %s/%s/%s.git %s",
                gitlabUrl,
                migration.getGitlabGroup(),
                migration.getSvnProject(),
                migration.getGitlabGroup());
            execCommand(rootWorkingDir, initCommand);

            String cloneCommand = String.format("git svn clone --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                migration.getSvnProject(),
                migration.getSvnProject(),
                migration.getSvnProject(),
                svnUrl,
                migration.getSvnGroup());
            execCommand(rootWorkingDir, cloneCommand);

            endStep(history);

            // 3. Clean large files
            history = startStep(migration, StepEnum.GIT_CLEANING, null);

            Main.main(new String[]{"--delete-files", "*.zip", "--no-blob-protection", gitWorkingDir});
            String gitCommand = "git reflog expire --expire=now --all && git gc --prune=now --aggressive";
            execCommand(gitWorkingDir, gitCommand);

            endStep(history);

            // 4. Reorganize repo
            // 4.1 Rename folder
            // 4.2 Set up target structure
            // 4.3 Git commit
            // 4.4 Git push
            history = startStep(migration, StepEnum.GIT_PUSH, null);

            String gitUrl = String.format("%s/%s/%s.git",
                gitlabUrl,
                migration.getGitlabGroup(),
                migration.getSvnProject());
            addRemote(git,"origin", gitUrl);
            addRemote(git,"gitlab", gitUrl);

            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(migration.getUser(), "XYbJSzgWxbuJKrfwaQ1Q"));
            pushCommand.call();

            endStep(history);

            migration.setStatus(StatusEnum.DONE);
            migrationRepository.save(migration);
        } catch (Exception exc) {
            if (history != null) {
                history.setStatus(StatusEnum.FAILED);
                migrationHistoryRepository.save(history);
            }

            migration.setStatus(StatusEnum.FAILED);
            migrationRepository.save(migration);
        }
    }

    private static String workingDir(Migration mig) {
        if (isWindows) {
            return System.getProperty("java.io.tmpdir") + "\\" + mig.getId();
        }
        return System.getProperty("java.io.tmpdir") + "/" + mig.getId();
    }

    private static String gitWorkingDir(Migration migration) {
        if (isWindows) {
            return workingDir(migration) + "\\" + migration.getSvnGroup() + "\\.git";
        }
        return workingDir(migration) + "/" + migration.getSvnGroup() + "/.git";
    }

    private static void execCommand(String directory, String command) throws InterruptedException, IOException {
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.directory(new File(directory));

        System.out.println(">>>>>>>>>>>>>>>>>>>>> Exec command : " + command);
        System.out.println(">>>>>>>>>>>>>>>>>>>>> in " + directory);

        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), System.err::println);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        int exitCode = process.waitFor();
        System.out.println(">>>>>>>>>>>>>>>>>>>>> Exit : " + exitCode);

        assert exitCode == 0;
    }

    // Tasks
    private static void addRemote(Git git, String remoteName, String remoteUrl) throws IOException, URISyntaxException, GitAPIException {
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName(remoteName);
        remoteAddCommand.setUri(new URIish(remoteUrl));
        remoteAddCommand.call();
    }

    // History management
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

    private void endStep(MigrationHistory history) {
        history.setStatus(StatusEnum.DONE);
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
