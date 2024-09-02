package fr.yodamad.svn2git.init

import com.google.common.io.Files.createTempDir
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import java.io.IOException
import java.lang.String.format
import java.util.*
import javax.annotation.PostConstruct
import kotlin.system.exitProcess

@Component
@Profile("dev")
class CheckUp {
    private val GIT_ERROR = "⛔️ svn2git requires Git v2.20+ or newer"

    private val GIT_SVN_VERSION = "git-svn version 2"
    private val GIT_SVN_ERROR = "⛔️ svn2git requires 'git svn' extension in v2+"

    private val SVN_VERSION = "svn"
    private val SVN_ERROR = "⛔️ svn2git requires 'svn' v1+"

    private val EXPECT_VERSION = "expect"
    private val EXPECT_ERROR = "⛔️ expect binary is required on Linux. 👉 Run apt-get|yum install expect."

    val isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows")

    @PostConstruct
    fun atStartup() {
        var allGood = checkGitSvnClone()
        allGood = allGood && checkCommand("git svn --version", GIT_SVN_VERSION, GIT_SVN_ERROR)
        allGood = allGood && checkCommand("svn --version", SVN_VERSION, SVN_ERROR)
        if (!isWindows) allGood = allGood && checkCommand("expect -version", EXPECT_VERSION, EXPECT_ERROR)
        if (!allGood) exitProcess(1)
    }

    @Throws(CheckUpException::class, InterruptedException::class, IOException::class)
    fun execCommand(command: String?): String {
        val execDir = createTempDir().absolutePath
        val builder = ProcessBuilder()
        val input = File.createTempFile("check_in", "up")
        val output = File.createTempFile("check_out", "up")
        val errput = File.createTempFile("check_err", "up")
        builder.redirectInput(input)
        builder.redirectOutput(output)
        builder.redirectError(errput)
        LOG.debug(String.format("Check up tools for svn2git in %s", execDir))
        if (isWindows) {
            builder.command("cmd.exe", "/c", command)
        } else {
            builder.command("sh", "-c", command)
        }
        builder.directory(File(execDir))
        val process = builder.start()
        val exitCode = process.waitFor()
        val sbf = StringBuilder()
        var scanner = Scanner(output)
        while (scanner.hasNextLine()) {
            val data = scanner.nextLine()
            sbf.append(data)
        }
        scanner.close()
        scanner = Scanner(errput)
        while (scanner.hasNextLine()) {
            val data = scanner.nextLine()
            LOG.error(data)
        }
        scanner.close()
        LOG.debug(sbf.toString())
        LOG.debug(format("Exit : %d", exitCode))
        if (exitCode != 0) {
            throw CheckUpException()
        }
        return sbf.toString()
    }

    private fun checkGitSvnClone(): Boolean {
        val result = execCommand("git --version")
        val regex = "git version 2\\.[2-9][0-9]\\.[0-9]".toRegex()
        return if (regex.containsMatchIn(result)) true
        else {
            LOG.error(GIT_ERROR)
            false
        }
    }

    private fun checkCommand(command: String, version: String, error: String): Boolean {
        try {
            if (!execCommand(command).startsWith(version)) {
                LOG.error(error)
                return false
            }
            return true
        } catch (checkUpException: CheckUpException) {
            LOG.error(error)
            return false
        } catch (checkUpException: InterruptedException) {
            LOG.error(error)
            return false
        } catch (checkUpException: IOException) {
            LOG.error(error)
            return false
        }
    }

    // Utils
    private class CheckUpException : Throwable()
    companion object {
        private val LOG = LoggerFactory.getLogger(CheckUp::class.java)
    }
}
