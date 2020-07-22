package fr.yodamad.svn2git.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

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

        String execDir = createTempDir().getAbsolutePath();

        ProcessBuilder builder = new ProcessBuilder();
        File input = File.createTempFile("check_in", "up");
        File output = File.createTempFile("check_out", "up");
        File errput = File.createTempFile("check_err", "up");

        builder.redirectInput(input);
        builder.redirectOutput(output);
        builder.redirectError(errput);

        LOG.debug(format("Check up tools for svn2git in %s", execDir));
        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        builder.directory(new File(execDir));

        Process process = builder.start();
        int exitCode = process.waitFor();

        StringBuilder sbf = new StringBuilder();
        Scanner scanner = new Scanner(output);
        while (scanner.hasNextLine()) {
            String data = scanner.nextLine();
            sbf.append(data);
        }
        scanner.close();

        scanner = new Scanner(errput);
        while (scanner.hasNextLine()) {
            String data = scanner.nextLine();
            LOG.error(data);
        }
        scanner.close();

        LOG.debug(sbf.toString());
        LOG.debug(format("Exit : %d", exitCode));
        if (exitCode != 0) {
            throw new CheckUpException();
        }
        return sbf.toString();
    }

    // Utils
    private static class CheckUpException extends Throwable { }
}
