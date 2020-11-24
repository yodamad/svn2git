package fr.yodamad.svn2git.io

import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.service.util.CommandManager
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.function.Consumer

/**
 * Shell utilities
 */
object Shell {
    private val LOG = LoggerFactory.getLogger(Shell::class.java)
    val isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows")

    /**
     * Get working directory
     * @param directory
     * @param mig
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    @Throws(IOException::class, InterruptedException::class, RuntimeException::class)
    fun workingDir(commandManager: CommandManager, directory: String, mig: Migration): String {
        val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        if (isWindows) {
            val path = String.format("%s%s", System.getenv("SystemDrive"), directory.replace("/".toRegex(), "\\\\"))
            if (!File(path).exists()) {
                val mkdir = String.format("mkdir %s", path)
                execCommand(commandManager, System.getenv("SystemDrive"), mkdir)
            }
            return directory + "\\" + today + "_" + mig.id
        }
        if (!File(directory).exists()) {
            val mkdir = String.format("mkdir -p %s", directory)
            execCommand(commandManager, "/", mkdir)
        }
        return directory + "/" + today + "_" + mig.id
    }

    /**
     * Get git working directory
     * @param root
     * @param sub
     * @return
     */
    fun gitWorkingDir(root: String, sub: String): String {
        return if (isWindows) {
            formatDirectory(root + "\\" + sub)
        } else "$root/$sub"
    }

    /**
     * Execute a commmand through process
     * @param directory Directory in which running command
     * @param command command to execute
     * @throws InterruptedException
     * @throws IOException
     */
    @JvmOverloads
    @Throws(InterruptedException::class, IOException::class)
    fun execCommand(commandManager: CommandManager, directory: String, command: String?, securedCommandToPrint: String? = command): Int {
        val builder = ProcessBuilder()
        val execDir = formatDirectory(directory)
        if (isWindows) {
            builder.command("cmd.exe", "/c", command)
        } else {
            builder.command("sh", "-c", command)
        }
        builder.directory(File(execDir))
        LOG.debug(String.format("Exec command : %s", securedCommandToPrint))
        LOG.debug(String.format("in %s", execDir))
        val process = builder.start()
        val streamGobbler = StreamGobbler(process.inputStream) { s: String? -> LOG.debug(s) }
        Executors.newSingleThreadExecutor().submit(streamGobbler)
        val errorStreamGobbler = StreamGobbler(process.errorStream) { s: String? -> LOG.debug(s) }
        Executors.newSingleThreadExecutor().submit(errorStreamGobbler)
        val stderr = IOUtils.toString(process.errorStream, Charset.defaultCharset())
        val exitCode = process.waitFor()
        LOG.debug(String.format("Exit : %d", exitCode))
        if (exitCode != 0) {
            // trace failed commands
            if (securedCommandToPrint != null) {
                commandManager.addFailedCommand(directory, securedCommandToPrint, stderr)
            }
            throw RuntimeException(stderr)
        } else {
            // trace successful commands
            if (securedCommandToPrint != null) {
                commandManager.addSuccessfulCommand(directory, securedCommandToPrint)
            }
        }
        return exitCode
    }

    /**
     * Format directory to fit f*** windows behavior
     * @param directory Directory to format
     * @return formatted directory
     */
    fun formatDirectory(directory: String): String {
        var execDir = directory
        if (isWindows) {
            execDir = if (directory.startsWith("/")) String.format("%s%s", System.getenv("SystemDrive"), directory).replace("/".toRegex(), "\\\\") else directory
        }
        return execDir
    }

    // Utils
    private class StreamGobbler constructor(private val inputStream: InputStream, private val consumer: Consumer<String?>) : Runnable {
        override fun run() {
            BufferedReader(InputStreamReader(inputStream)).lines()
                .forEach(consumer)
        }
    }
}
