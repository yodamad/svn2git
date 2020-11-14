package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.data.CleanedFiles
import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.MigrationRemovedFile
import fr.yodamad.svn2git.domain.enumeration.Reason
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.service.CleanedFilesManager
import fr.yodamad.svn2git.service.MigrationRemovedFileService
import net.steppschuh.markdowngenerator.table.Table
import net.steppschuh.markdowngenerator.text.emphasis.BoldText
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText
import net.steppschuh.markdowngenerator.text.heading.Heading
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.function.Consumer

/**
 * Markdown generator
 */
@Service
open class MarkdownGenerator(
    private val applicationProperties: ApplicationProperties,
    private val migrationRemovedFileService: MigrationRemovedFileService) {
    private val log = LoggerFactory.getLogger(MarkdownGenerator::class.java)

    /**
     * Genereate content of README.md for summary
     *
     * @param migration Migration done
     * @param cleanedFilesManager
     * @return markdown string
     */
    @Throws(IOException::class)
    open fun generateSummaryReadme(migration: Migration, cleanedFilesManager: CleanedFilesManager, workUnit: WorkUnit) {
        val md = StringBuilder()
        // Overview
        md.append(Heading(migration.gitlabProject.toUpperCase().replace("/".toRegex(), ""), 1))
            .append(EMPTY_LINE)
            .append(String.format(" migrated from %s%s%s to %s%s",
                migration.svnUrl, migration.svnGroup, migration.svnProject,
                migration.gitlabUrl, migration.gitlabGroup))
            .append(EMPTY_LINE)
            .append(String.format(" gitlab user %s on %s",
                BoldText(ItalicText(if (migration.gitlabToken == null) applicationProperties.gitlab.account.toUpperCase() else migration.user.toUpperCase())),
                migration.date))
            .append(EMPTY_LINE)
            .append(String.format(" subversion user %s on %s",
                BoldText(ItalicText(migration.svnUser.toUpperCase())),
                migration.date))
            .append(EMPTY_LINE)
        if (migration.mappings.isNotEmpty()) {
            // Mapping
            md.append(Heading("Mappings applied", 3)).append(EMPTY_LINE)
            val mappingTableBuilder = Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
                .addRow("SVN directory", "Regex", "GIT directory", "Not Migrated from SVN")
            migration.mappings.forEach(Consumer { map: Mapping -> mappingTableBuilder.addRow(map.svnDirectory, map.regex, map.gitDirectory, map.isSvnDirectoryDelete) })
            md.append(mappingTableBuilder.build()).append(EMPTY_LINE)
        }
        if (!StringUtils.isEmpty(migration.forbiddenFileExtensions) ||
            !StringUtils.isEmpty(migration.maxFileSize) && Character.isDigit(migration.maxFileSize[0])) {
            // Cleaning
            md.append(Heading("Cleaning options", 3)).append(EMPTY_LINE)
            val cleaningTableBuilder = Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                .addRow("Option", "Value")
            if (!StringUtils.isEmpty(migration.maxFileSize)) {
                cleaningTableBuilder.addRow("Max file size", migration.maxFileSize)
            }
            if (!StringUtils.isEmpty(migration.forbiddenFileExtensions) && Character.isDigit(migration.maxFileSize[0])) {
                cleaningTableBuilder.addRow("Files extension(s) removed", migration.forbiddenFileExtensions)
            }
            if (!StringUtils.isEmpty(migration.svnHistory)) {
                cleaningTableBuilder.addRow("Subversion History", migration.svnHistory)
            }
            if (!StringUtils.isEmpty(migration.branchesToMigrate)) {
                cleaningTableBuilder.addRow("Branches to migrate", migration.branchesToMigrate)
            }
            if (!StringUtils.isEmpty(migration.tagsToMigrate)) {
                cleaningTableBuilder.addRow("Tags to migrate", migration.tagsToMigrate)
            }
            md.append(cleaningTableBuilder.build()).append(EMPTY_LINE)

            // *********** SVN Location File Size Report ******************
            md.append(Heading(String.format("File Size Totals: SVN (%s), Gitlab (%s)",
                BytesConverterUtil.humanReadableByteCount(cleanedFilesManager.fileSizeTotalBeforeClean, false),
                BytesConverterUtil.humanReadableByteCount(cleanedFilesManager.fileSizeTotalAfterClean, false)
            ), 3))
                .append(EMPTY_LINE)
            val totalFileSizeTableBuilder = Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
                .addRow("SVN Location", "Number of Files SVN", "Total File Size SVN", "Number of Files Gitlab", "Total File Size Gitlab")
            cleanedFilesManager.cleanedReportMap.forEach { (_: String?, value: CleanedFiles) ->
                totalFileSizeTableBuilder.addRow(
                    value.svnLocation,
                    value.fileCountBeforeClean,
                    BytesConverterUtil.humanReadableByteCount(value.fileSizeTotalBeforeClean, false),
                    value.fileCountAfterClean,
                    BytesConverterUtil.humanReadableByteCount(value.fileSizeTotalAfterClean, false)
                )
            }
            md.append(totalFileSizeTableBuilder.build()).append(EMPTY_LINE)

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : Extension Reason
            val migrationRemovedFilesReasonExtension = migrationRemovedFileService.findAllForMigrationAndReason(migration.id, Reason.EXTENSION)
            log.debug(String.format("0:migrationRemovedFilesReasonExtension.size():%s", migrationRemovedFilesReasonExtension.size))
            if (migrationRemovedFilesReasonExtension.isNotEmpty()) {
                md.append(Heading("Migration Removed Files : Reason EXTENSION", 3)).append(EMPTY_LINE)
                val migrationRemovedFileReasonExtensionTableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "fileSize")
                migrationRemovedFilesReasonExtension.forEach(Consumer { migrationRemovedFile: MigrationRemovedFile -> addRemovedFileRow(migrationRemovedFileReasonExtensionTableBuilder, migrationRemovedFile) })
                md.append(migrationRemovedFileReasonExtensionTableBuilder.build()).append(EMPTY_LINE)
            }

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : SIZE Reason
            val migrationRemovedFilesReasonSize = migrationRemovedFileService.findAllForMigrationAndReason(migration.id, Reason.SIZE)
            log.debug(String.format("1:migrationRemovedFilesReasonSize.size():%s", migrationRemovedFilesReasonSize.size))
            if (migrationRemovedFilesReasonSize.isNotEmpty()) {
                md.append(Heading("Migration Removed Files : Reason SIZE", 3)).append(EMPTY_LINE)
                val migrationRemovedFileReasonSizeTableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "fileSize")
                migrationRemovedFilesReasonSize.forEach(Consumer { migrationRemovedFile: MigrationRemovedFile -> addRemovedFileRow(migrationRemovedFileReasonSizeTableBuilder, migrationRemovedFile) })
                md.append(migrationRemovedFileReasonSizeTableBuilder.build()).append(EMPTY_LINE)
            }
        }

        // History

        // get first and last emelent of the migrationHistory set (which is ordered by ID)
        var firstElement: MigrationHistory? = null
        var lastElement: MigrationHistory? = null
        val iterator: Iterator<MigrationHistory> = migration.histories.iterator()
        while (iterator.hasNext()) {
            val migrationHistoryElement = iterator.next()
            if (firstElement == null) {
                firstElement = migrationHistoryElement
            }
            if (!iterator.hasNext()) {
                lastElement = migrationHistoryElement
            }
        }
        var minutesDifference: Long = 0
        if (firstElement != null && lastElement != null) {
            minutesDifference = ChronoUnit.MINUTES.between(firstElement.date, lastElement.date)
        }

        // TIME
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.FRANCE)
            .withZone(ZoneId.systemDefault())
        md.append(Heading(String.format("Migration steps history (duration: %s minutes)", minutesDifference), 3)).append(EMPTY_LINE)
        val historyTableBuilder = Table.Builder()
            .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
            .addRow("Step", "Status", "Time (dd/MM/yy)", "Details", "Execution Time")

        // Write all to file at this point (because StringBuffer is limited in what it can handle)...
        Files.write(Paths.get(workUnit.directory, "README.md"), md.toString().toByteArray(), StandardOpenOption.CREATE)
        migration.histories.forEach(Consumer { h: MigrationHistory ->
            historyTableBuilder.addRow(h.step, h.status,
                formatter.format(h.date), getHistoryDetails(h), h.executionTime)
        })
        Files.write(Paths.get(workUnit.directory, "README.md"), historyTableBuilder.build().toString().toByteArray(), StandardOpenOption.APPEND)
    }

    /**
     * Handle conditions when generating data for columns in History section
     * @param migHistory MigrationHistory object
     * @return string value to add to details column
     */
    private fun getHistoryDetails(migHistory: MigrationHistory): String {
        return when {
            migHistory.step == StepEnum.LIST_REMOVED_FILES -> {
                "See Migration Removed Files Section Above"
            }
            StringUtils.isNotEmpty(migHistory.data) -> {
                migHistory.data.replace("|", "&#124;")
            }
            else -> {
                ""
            }
        }
    }

    open fun addRemovedFileRow(migrationRemovedFileTableBuilder: Table.Builder, migrationRemovedFile: MigrationRemovedFile) {
        var id = ""
        if (migrationRemovedFile.id != null) {
            id = migrationRemovedFile.id.toString()
        }
        var path = ""
        if (!StringUtils.isEmpty(migrationRemovedFile.path)) {
            path = migrationRemovedFile.path
        }
        var svnLocation = ""
        if (!StringUtils.isEmpty(migrationRemovedFile.svnLocation)) {
            svnLocation = migrationRemovedFile.svnLocation
        }
        var fileSize: String? = ""
        if (migrationRemovedFile.fileSize != null) {
            fileSize = BytesConverterUtil.humanReadableByteCount(migrationRemovedFile.fileSize, false)
        }
        if (applicationProperties.artifactory.enabled) {
            // Remove leading slash from artifactory.binariesDirectory
            val binariresDirectory = applicationProperties.artifactory.binariesDirectory.substring(1)
            val binInTags = path.startsWith(binariresDirectory) && svnLocation.startsWith("tags")
            migrationRemovedFileTableBuilder.addRow(id,
                if (binInTags) path else BoldText(ItalicText(path)),
                svnLocation, if (binInTags) "Yes" else "", fileSize)
        } else {
            migrationRemovedFileTableBuilder.addRow(id,
                path,
                svnLocation,
                "Yes",
                fileSize)
        }
    }

    companion object {
        private const val EMPTY_LINE = "\n\n"
    }
}
