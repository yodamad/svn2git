package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.domain.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static fr.yodamad.svn2git.service.util.MigrationConstants.JAVA_IO_TMPDIR;
import static java.lang.String.format;

/**
 * Shell utilities
 */
public abstract class Shell {

    private static final Logger LOG = LoggerFactory.getLogger(Shell.class);

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * Get working directory
     * @param mig
     * @return
     */
    public static String workingDir(Migration mig) {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (isWindows) {
            return System.getProperty(JAVA_IO_TMPDIR) + "\\" + today + "_" + mig.getId();
        }
        return System.getProperty(JAVA_IO_TMPDIR) + "/" + today + "_" + mig.getId();
    }

    /**
     * Get git working directory
     * @param root
     * @param sub
     * @return
     */
    public static String gitWorkingDir(String root, String sub) {
        if (isWindows) {
            return root + "\\" + sub;
        }
        return root + "/" + sub;
    }

    /**
     * Execute a commmand through process
     * @param directory Directory in which running command
     * @param command command to execute
     * @throws InterruptedException
     * @throws IOException
     */
    public static int execCommand(String directory, String command) throws InterruptedException, IOException {
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
    public static int execCommand(String directory, String command, String securedCommandToPrint) throws InterruptedException, IOException {
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
