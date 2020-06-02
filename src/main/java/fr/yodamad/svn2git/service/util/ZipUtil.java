package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.domain.WorkUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Some utilities methods to work with zip
 */
public class ZipUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ZipUtil.class);

    /**
     * Zip a directory
     *
     * @param directory
     * @param archiveName
     * @return zip file name
     * @throws IOException
     */
    public static String zipDirectory(WorkUnit workUnit, Path directory, String archiveName) throws IOException {

        // zipName is the name of the tag
        String zipName = String.format("%s.zip", archiveName);

        // zip stored in workUnit.root
        String zipFilePath = String.format("%s/%s", workUnit.root, zipName);
        LOG.info(String.format("ZipFilePath:%s", zipFilePath));

        // Outputstream for zipName
        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;

        try {

            fileOutputStream = new FileOutputStream(zipFilePath);

            // Entry names and comments in the Zip will use UTF-8
            zipOutputStream = new ZipOutputStream(fileOutputStream, StandardCharsets.UTF_8);

            // Initial value taken from application.yml artifactory.binariesDirectory
            File fileToZip = directory.toFile();

            // Start Zip Process starting from /05_impl/1_bin
            zipFile(fileToZip, fileToZip.getName(), zipOutputStream, true);

        } finally {
            // Close everything
            if (zipOutputStream != null) {
                zipOutputStream.close();
            }

            if (fileOutputStream != null) {
                fileOutputStream.close();
            }

        }

        return zipFilePath;
    }

    /**
     * Zip a file
     *
     * @param fileToZip
     * @param fileName
     * @param zipOut
     * @throws IOException
     */
    public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, boolean skip) throws IOException {

        // Hidden Files Not Zipped
        if (fileToZip.isHidden()) {
            return;
        }

        if (fileToZip.isDirectory()) {

            // Create Folder in ZipOutputStream
            if (!skip) {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    zipOut.closeEntry();
                } else {
                    zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                    zipOut.closeEntry();
                }
            }

            // Get list of files and folders in this folder
            try (Stream<Path> files = Files.list(fileToZip.toPath())) {

                // For each file of folder
                // Note : using path getFileName and not file.getName (issues with encoding / filenames with accents)
                files.
                    forEach(path -> {
                        try {
                            zipFile(path.toFile(), (skip ? "" : fileName + "/") + path.getFileName(), zipOut, false);
                        } catch (IOException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    });
            }

            return;
        }

        // create zipEntry using fileName which should be ok even for accented characters
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);

        // Get byte array of file (we do not want to change anything
        byte[] fileByteArray = Files.readAllBytes(fileToZip.toPath());
        zipOut.write(fileByteArray);

    }
}
