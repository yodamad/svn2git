package fr.yodamad.svn2git.io

import fr.yodamad.svn2git.data.WorkUnit
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Some utilities methods to work with zip
 */
object ZipUtil {
    private val LOG = LoggerFactory.getLogger(ZipUtil::class.java)

    /**
     * Zip a directory
     *
     * @param directory
     * @param archiveName
     * @return zip file name
     * @throws IOException
     */
    @Throws(IOException::class)
    fun zipDirectory(workUnit: WorkUnit, directory: Path, archiveName: String?): String {

        // zipName is the name of the tag
        val zipName = String.format("%s.zip", archiveName)

        // zip stored in workUnit.root
        val zipFilePath = String.format("%s/%s", workUnit.root, zipName)
        LOG.info(String.format("ZipFilePath:%s", zipFilePath))

        // Outputstream for zipName
        var fileOutputStream: FileOutputStream? = null
        var zipOutputStream: ZipOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(zipFilePath)

            // Entry names and comments in the Zip will use UTF-8
            zipOutputStream = ZipOutputStream(fileOutputStream, StandardCharsets.UTF_8)

            // Initial value taken from application.yml artifactory.binariesDirectory
            val fileToZip = directory.toFile()

            // Start Zip Process starting from /05_impl/1_bin
            zipFile(fileToZip, fileToZip.name, zipOutputStream, true)
        } finally {
            // Close everything
            zipOutputStream?.close()
            fileOutputStream?.close()
        }
        return zipFilePath
    }

    /**
     * Zip a file
     *
     * @param fileToZip
     * @param fileName
     * @param zipOut
     * @throws IOException
     */
    @Throws(IOException::class)
    fun zipFile(fileToZip: File, fileName: String, zipOut: ZipOutputStream, skip: Boolean) {

        // Hidden Files Not Zipped
        if (fileToZip.isHidden) {
            return
        }
        if (fileToZip.isDirectory) {

            // Create Folder in ZipOutputStream
            if (!skip) {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(ZipEntry(fileName))
                    zipOut.closeEntry()
                } else {
                    zipOut.putNextEntry(ZipEntry("$fileName/"))
                    zipOut.closeEntry()
                }
            }
            val files = Files.list(fileToZip.toPath())
            // For each file of folder
            // Note : using path getFileName and not file.getName (issues with encoding / filenames with accents)
            files.forEach { path: Path ->
                try {
                    zipFile(path.toFile(), (if (skip) "" else "$fileName/") + path.fileName, zipOut, false)
                } catch (e: IOException) {
                    LOG.error(e.message, e)
                }
            }
            return
        }

        // create zipEntry using fileName which should be ok even for accented characters
        val zipEntry = ZipEntry(fileName)
        zipOut.putNextEntry(zipEntry)

        // Get byte array of file (we do not want to change anything
        val fileByteArray = Files.readAllBytes(fileToZip.toPath())
        zipOut.write(fileByteArray)
    }
}
