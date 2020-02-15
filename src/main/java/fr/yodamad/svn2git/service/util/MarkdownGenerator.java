package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.Reason;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.service.CleanedFilesManager;
import fr.yodamad.svn2git.service.MigrationRemovedFileService;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

/**
 * Markdown generator
 */
@Service
public class MarkdownGenerator {

    private final Logger log = LoggerFactory.getLogger(MarkdownGenerator.class);

    private static final String EMPTY_LINE = "\n\n";

    // MigrationRemoveFileService
    private final MigrationRemovedFileService migrationRemovedFileService;

    // Configuration
    private final ApplicationProperties applicationProperties;

    public MarkdownGenerator(ApplicationProperties applicationProperties, MigrationRemovedFileService migrationRemovedFileService) {
        this.migrationRemovedFileService = migrationRemovedFileService;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Genereate content of README.md for summary
     *
     * @param migration Migration done
     * @param cleanedFilesManager
     * @return markdown string
     */
    public void generateSummaryReadme(Migration migration, CleanedFilesManager cleanedFilesManager, WorkUnit workUnit) throws IOException {

        StringBuilder md = new StringBuilder();
        // Overview
        md.append(new Heading(migration.getGitlabProject().toUpperCase().replaceAll("/", ""), 1))
            .append(EMPTY_LINE)
            .append(format(" migrated from %s%s%s to %s%s",
                migration.getSvnUrl(), migration.getSvnGroup(), migration.getSvnProject(),
                migration.getGitlabUrl(), migration.getGitlabGroup()))
            .append(EMPTY_LINE)
            .append(format(" gitlab user %s on %s",
                new BoldText(new ItalicText((migration.getGitlabToken() == null ? this.applicationProperties.gitlab.account.toUpperCase() : migration.getUser().toUpperCase()))),
                migration.getDate()))
            .append(EMPTY_LINE)
            .append(format(" subversion user %s on %s",
            new BoldText(new ItalicText(migration.getSvnUser().toUpperCase())),
            migration.getDate()))
            .append(EMPTY_LINE);

        if (!migration.getMappings().isEmpty()) {
            // Mapping
            md.append(new Heading("Mappings applied", 3)).append(EMPTY_LINE);
            Table.Builder mappingTableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
                .addRow("SVN directory", "Regex", "GIT directory", "Not Migrated from SVN");
            migration.getMappings().stream().forEach(map -> mappingTableBuilder.addRow(map.getSvnDirectory(), map.getRegex(), map.getGitDirectory(), map.isSvnDirectoryDelete()));
            md.append(mappingTableBuilder.build()).append(EMPTY_LINE);
        }

        if (!StringUtils.isEmpty(migration.getForbiddenFileExtensions()) ||
            (!StringUtils.isEmpty(migration.getMaxFileSize()) && Character.isDigit(migration.getMaxFileSize().charAt(0)))) {
            // Cleaning
            md.append(new Heading("Cleaning options", 3)).append(EMPTY_LINE);
            Table.Builder cleaningTableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                .addRow("Option", "Value");

            if (!StringUtils.isEmpty(migration.getMaxFileSize())) {
                cleaningTableBuilder.addRow("Max file size", migration.getMaxFileSize());
            }
            if (!StringUtils.isEmpty(migration.getForbiddenFileExtensions()) && Character.isDigit(migration.getMaxFileSize().charAt(0))) {
                cleaningTableBuilder.addRow("Files extension(s) removed", migration.getForbiddenFileExtensions());
            }

            if (!StringUtils.isEmpty(migration.getSvnHistory())) {
                cleaningTableBuilder.addRow("Subversion History", migration.getSvnHistory());
            }

            if (!StringUtils.isEmpty(migration.getBranchesToMigrate())) {
                cleaningTableBuilder.addRow("Branches to migrate", migration.getBranchesToMigrate());
            }

            if (!StringUtils.isEmpty(migration.getTagsToMigrate())) {
                cleaningTableBuilder.addRow("Tags to migrate", migration.getTagsToMigrate());
            }

            md.append(cleaningTableBuilder.build()).append(EMPTY_LINE);

            // *********** SVN Location File Size Report ******************

            md.append(new Heading(String.format("File Size Totals: SVN (%s), Gitlab (%s)",
                BytesConverterUtil.humanReadableByteCount(cleanedFilesManager.getFileSizeTotalBeforeClean(), false),
                BytesConverterUtil.humanReadableByteCount(cleanedFilesManager.getFileSizeTotalAfterClean(), false)
                ), 3))
                .append(EMPTY_LINE);
            Table.Builder totalFileSizeTableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
                .addRow("SVN Location", "Number of Files SVN", "Total File Size SVN", "Number of Files Gitlab", "Total File Size Gitlab");

            cleanedFilesManager.
                getCleanedReportMap().
                entrySet().
                stream().
                forEach(entry -> {
                    totalFileSizeTableBuilder.addRow(
                        entry.getValue().getSvnLocation(),
                        entry.getValue().getFileCountBeforeClean(),
                        BytesConverterUtil.humanReadableByteCount(entry.getValue().getFileSizeTotalBeforeClean(), false),
                        entry.getValue().getFileCountAfterClean(),
                        BytesConverterUtil.humanReadableByteCount(entry.getValue().getFileSizeTotalAfterClean(), false)
                    );
                });

            md.append(totalFileSizeTableBuilder.build()).append(EMPTY_LINE);

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : Extension Reason
            List<MigrationRemovedFile> migrationRemovedFilesReasonExtension = migrationRemovedFileService.
                findAllForMigrationAndReason(migration.getId(), Reason.EXTENSION);

            log.debug(format("0:migrationRemovedFilesReasonExtension.size():%s", migrationRemovedFilesReasonExtension.size()));

            if (migrationRemovedFilesReasonExtension != null && !migrationRemovedFilesReasonExtension.isEmpty()) {

                md.append(new Heading("Migration Removed Files : Reason EXTENSION", 3)).append(EMPTY_LINE);
                Table.Builder migrationRemovedFileReasonExtensionTableBuilder = new Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "Reason", "fileSize");

                migrationRemovedFilesReasonExtension.stream().forEach(migrationRemovedFile -> {
                    addRemovedFileRow(migrationRemovedFileReasonExtensionTableBuilder, migrationRemovedFile);
                });

                md.append(migrationRemovedFileReasonExtensionTableBuilder.build()).append(EMPTY_LINE);
            }

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : SIZE Reason
            List<MigrationRemovedFile> migrationRemovedFilesReasonSize = migrationRemovedFileService.
                findAllForMigrationAndReason(migration.getId(), Reason.SIZE);

            log.debug(format("1:migrationRemovedFilesReasonSize.size():%s", migrationRemovedFilesReasonSize.size()));

            if (migrationRemovedFilesReasonSize != null && !migrationRemovedFilesReasonSize.isEmpty()) {

                md.append(new Heading("Migration Removed Files : Reason SIZE", 3)).append(EMPTY_LINE);
                Table.Builder migrationRemovedFileReasonSizeTableBuilder = new Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "Reason", "fileSize");

                migrationRemovedFilesReasonSize.stream().forEach(migrationRemovedFile -> {
                    addRemovedFileRow(migrationRemovedFileReasonSizeTableBuilder, migrationRemovedFile);
                });

                md.append(migrationRemovedFileReasonSizeTableBuilder.build()).append(EMPTY_LINE);

            }

        }

        // History

        // get first and last emelent of the migrationHistory set (which is ordered by ID)
        MigrationHistory firstElement = null;
        MigrationHistory lastElement = null;
        for (Iterator<MigrationHistory> iterator = migration.getHistories().iterator(); iterator.hasNext(); ) {
            MigrationHistory migrationHistoryElement =  iterator.next();
            if (firstElement == null) {
                firstElement = migrationHistoryElement;
            }
            if (!iterator.hasNext()) {
                lastElement = migrationHistoryElement;
            }
        }

        long minutesDifference = 0;
        if (firstElement != null && lastElement != null) {
            minutesDifference = ChronoUnit.MINUTES.between(firstElement.getDate(), lastElement.getDate());
        }

        // TIME
        DateTimeFormatter formatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withLocale(Locale.FRANCE)
                .withZone(ZoneId.systemDefault());

        md.append(new Heading(String.format("Migration steps history (duration: %s minutes)", minutesDifference), 3)).append(EMPTY_LINE);

        Table.Builder historyTableBuilder = new Table.Builder()
            .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
            .addRow("Step", "Status", "Time (dd/MM/yy)" ,"Details");

        log.debug("======>Before generate history section");

        // Write all to file at this point (because StringBuffer is limited in what it can handle)...
        Path readMeFilePath = Files.write(Paths.get(workUnit.directory, "README.md"), md.toString().getBytes(), StandardOpenOption.CREATE);

        migration.getHistories().stream().forEach(h -> historyTableBuilder.addRow(h.getStep(), h.getStatus(),
            formatter.format(h.getDate()), getHistoryDetails(h)));

        readMeFilePath = Files.write(Paths.get(workUnit.directory, "README.md"), historyTableBuilder.
                build().toString().getBytes(), StandardOpenOption.APPEND);

        log.debug("======>After generate history section");

    }

    /**
     * Handle conditions when generating data for columns in History section
     * @param migHistory MigrationHistory object
     * @return string value to add to details column
     */
    private String getHistoryDetails(MigrationHistory migHistory) {

        if (migHistory.getStep().equals(StepEnum.LIST_REMOVED_FILES)) {
            return "See Migration Removed Files Section Above";
        } else if (StringUtils.isNotEmpty(migHistory.getData())) {
            return migHistory.getData().replace("|", "&#124;");
        } else {
            return "";
        }

    }

    private void addRemovedFileRow(Table.Builder migrationRemovedFileTableBuilder, MigrationRemovedFile migrationRemovedFile) {

        String id = "";
        if (migrationRemovedFile.getId() != null) {
            id = migrationRemovedFile.getId().toString();
        }

        String path = "";
        if (!StringUtils.isEmpty(migrationRemovedFile.getPath())) {
            path = migrationRemovedFile.getPath();
        }

        String svnLocation = "";
        if (!StringUtils.isEmpty(migrationRemovedFile.getSvnLocation())) {
            svnLocation = migrationRemovedFile.getSvnLocation();
        }

        String reason = "";
        if (migrationRemovedFile.getReason() != null) {
            reason = migrationRemovedFile.getReason().toString();
        }

        String fileSize = "";
        if (migrationRemovedFile.getFileSize() != null) {
            fileSize = BytesConverterUtil.humanReadableByteCount(migrationRemovedFile.getFileSize(), false);
        }

        // Remove leading slash from artifactory.binariesDirectory
        String binariresDirectory = applicationProperties.artifactory.binariesDirectory.substring(1);
        migrationRemovedFileTableBuilder.addRow(id,
            ((path.startsWith(binariresDirectory) && svnLocation.startsWith("tags")) ? path : new BoldText(new ItalicText(path))),
            svnLocation,
            ((path.startsWith(binariresDirectory) && svnLocation.startsWith("tags")) ? "Yes" : ""),
            reason,
            fileSize);
    }
}

