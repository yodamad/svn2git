package fr.yodamad.svn2git.init;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.google.common.io.Files.createTempDir;
import static java.lang.String.format;

@Component
public class CheckUp {

    private static final String GIT_VERSION = "git version 2.";
    private static final String GIT_SVN_VERSION = "git-svn version 2";

    private static final Logger LOG = LoggerFactory.getLogger(CheckUp.class);
    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    @PostConstruct
    public void atStartup() {
        boolean allGood = true;
        try {
            if (!execCommand("git --version").startsWith(GIT_VERSION)) {
                LOG.error("⛔️ svn2git requires Git v2+");
                allGood = false;
            }
        } catch (CheckUpException | InterruptedException | IOException checkUpException) {
            LOG.error("⛔️ svn2git requires Git v2+");
            allGood = false;
        }

        try {
            if (!execCommand("git svn --version").startsWith(GIT_SVN_VERSION)) {
                LOG.error("⛔️ svn2git requires 'git svn' extension in v2+");
                allGood = false;
            }
        } catch (CheckUpException | InterruptedException | IOException checkUpException) {
            LOG.error("⛔️ svn2git requires 'git svn' extension in v2+");
            allGood = false;
        }

        if (!allGood) System.exit(1);
    }

    public static String execCommand(String command) throws CheckUpException, InterruptedException, IOException {

        ProcessBuilder builder = new ProcessBuilder();
        String execDir = createTempDir().getAbsolutePath();
        LOG.debug(format("Check up tools for svn2git in %s", execDir));
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.directory(new File(execDir));

        Process process = builder.start();
        AtomicReference<String> result = new AtomicReference<>();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), result::set);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        int exitCode = process.waitFor();
        LOG.debug(format("Exit : %d", exitCode));
        if (exitCode != 0) {
            throw new CheckUpException();
        }
        return result.get();

    }

    // Utils
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

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

    private static class CheckUpException extends Throwable {
    }
}
