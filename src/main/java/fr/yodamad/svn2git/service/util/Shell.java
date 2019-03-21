package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.domain.Migration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static java.lang.String.format;

/**
 * Shell utilities
 */
public abstract class Shell {

    private static final Logger LOG = LoggerFactory.getLogger(Shell.class);

    public static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    /**
     * Get working directory
     * @param directory
     * @param mig
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static String workingDir(String directory, Migration mig) throws IOException, InterruptedException, RuntimeException {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        if (isWindows) {
            String path = format("%s%s", System.getenv("SystemDrive"), directory.replaceAll("/", "\\\\"));
            if (!new File(path).exists()) {
                String mkdir = format("mkdir %s", path);
                execCommand(System.getenv("SystemDrive"), mkdir);
            }
            return directory + "\\" + today + "_" + mig.getId();
        }
        if (!new File(directory).exists()) {
            String mkdir = format("mkdir -p %s", directory);
            execCommand("/", mkdir);
        }
        return directory + "/" + today + "_" + mig.getId();
    }

    /**
     * Get git working directory
     * @param root
     * @param sub
     * @return
     */
    public static String gitWorkingDir(String root, String sub) {
        if (isWindows) {
            return Shell.formatDirectory(root + "\\" + sub);
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
        String execDir = formatDirectory(directory);
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.directory(new File(execDir));

        LOG.debug(format("Exec command : %s", securedCommandToPrint));
        LOG.debug(format("in %s", execDir));

        Process process = builder.start();
        StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(streamGobbler);

        StreamGobbler errorStreamGobbler = new StreamGobbler(process.getErrorStream(), LOG::debug);
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler);

        String stderr = IOUtils.toString(process.getErrorStream(), Charset.defaultCharset());

        int exitCode = process.waitFor();
        LOG.debug(format("Exit : %d", exitCode));

        if (exitCode != 0) {
            throw new RuntimeException(stderr);
        }

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

    /**
     * Format directory to fit f*** windows behavior
     * @param directory Directory to format
     * @return formatted directory
     */
    public static String formatDirectory(String directory) {
        String execDir = directory;
        if (isWindows) {
            execDir = directory.startsWith("/") ?
                format("%s%s", System.getenv("SystemDrive"), directory).replaceAll("/", "\\\\")
                : directory;
        }
        return execDir;
    }
}
