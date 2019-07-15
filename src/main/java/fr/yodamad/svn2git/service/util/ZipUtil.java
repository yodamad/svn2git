package fr.yodamad.svn2git.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Some utilities methods to work with zip
 */
public class ZipUtil {

    /**
     * Zip a directory
     * @param directory
     * @param archiveName
     * @return zip file name
     * @throws IOException
     */
    public static String zipDirectory(Path directory, String archiveName) throws IOException {
        String zipName = String.format("%s.zip", archiveName);
        FileOutputStream fos = new FileOutputStream(zipName);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = directory.toFile();

        zipFile(fileToZip, fileToZip.getName(), zipOut, true);
        zipOut.close();
        fos.close();
        return zipName;
    }

    /**
     * Zip a file
     * @param fileToZip
     * @param fileName
     * @param zipOut
     * @throws IOException
     */
    public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, boolean skip) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (!skip) {
                if (fileName.endsWith("/")) {
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    zipOut.closeEntry();
                } else {
                    zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                    zipOut.closeEntry();
                }
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, (skip ? "" : fileName + "/") + childFile.getName(), zipOut, false);
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }
}
