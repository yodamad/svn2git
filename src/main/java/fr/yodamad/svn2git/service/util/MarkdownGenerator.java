package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.domain.enumeration.Reason;
import fr.yodamad.svn2git.service.MigrationRemovedFileService;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.String.format;

/**
 * Markdown generator
 */
@Service
public class MarkdownGenerator {

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
     * @return markdown string
     */
    public String generateSummaryReadme(Migration migration) {
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

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : Extension Reason
            List<MigrationRemovedFile> migrationRemovedFilesReasonExtension = migrationRemovedFileService.
                findAllForMigrationAndReason(migration.getId(), Reason.EXTENSION);

            if (migrationRemovedFilesReasonExtension != null && !migrationRemovedFilesReasonExtension.isEmpty()) {

                md.append(new Heading("Migration Removed Files : Reason EXTENSION", 3)).append(EMPTY_LINE);
                Table.Builder migrationRemovedFileReasonExtensionTableBuilder = new Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "Reason", "fileSize(bytes)");

                migrationRemovedFilesReasonExtension.stream().forEach(migrationRemovedFile -> {
                    addRemovedFileRow(migrationRemovedFileReasonExtensionTableBuilder, migrationRemovedFile);
                });

                md.append(migrationRemovedFileReasonExtensionTableBuilder.build()).append(EMPTY_LINE);
            }

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : SIZE Reason
            List<MigrationRemovedFile> migrationRemovedFilesReasonSize = migrationRemovedFileService.
                findAllForMigrationAndReason(migration.getId(), Reason.SIZE);

            if (migrationRemovedFilesReasonSize != null && !migrationRemovedFilesReasonSize.isEmpty()) {

                md.append(new Heading("Migration Removed Files : Reason SIZE", 3)).append(EMPTY_LINE);
                Table.Builder migrationRemovedFileReasonSizeTableBuilder = new Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "Reason", "fileSize(bytes)");

                migrationRemovedFilesReasonSize.stream().forEach(migrationRemovedFile -> {
                    addRemovedFileRow(migrationRemovedFileReasonSizeTableBuilder, migrationRemovedFile);
                });

                md.append(migrationRemovedFileReasonSizeTableBuilder.build()).append(EMPTY_LINE);

            }

        }

        // History
        md.append(new Heading("Migration steps history", 3)).append(EMPTY_LINE);
        Table.Builder historyTableBuilder = new Table.Builder()
            .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
            .addRow("Step", "Status", "Details");
        migration.getHistories().stream().forEach(h -> historyTableBuilder.addRow(h.getStep(), h.getStatus(), (StringUtils.isNotEmpty(h.getData()) ? h.getData().replace("|", "&#124;") : "")));
        md.append(historyTableBuilder.build()).append(EMPTY_LINE);

        return md.toString();
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
            fileSize = migrationRemovedFile.getFileSize().toString();
        }

        migrationRemovedFileTableBuilder.addRow(id,
            (path.startsWith(applicationProperties.artifactory.binariesDirectory) ? path : new BoldText(new ItalicText(path))),
            svnLocation,
            (path.startsWith(applicationProperties.artifactory.binariesDirectory) ? "Yes" : ""),
            reason, fileSize);
    }
}

