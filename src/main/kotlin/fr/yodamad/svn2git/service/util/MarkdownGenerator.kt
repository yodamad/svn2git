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
import fr.yodamad.svn2git.functions.EMPTY
import fr.yodamad.svn2git.io.BytesConverterUtil.humanReadableByteCount
import fr.yodamad.svn2git.service.CleanedFilesManager
import fr.yodamad.svn2git.service.MigrationRemovedFileService
import net.steppschuh.markdowngenerator.table.Table
import net.steppschuh.markdowngenerator.text.emphasis.BoldText
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText
import net.steppschuh.markdowngenerator.text.heading.Heading
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.isEmpty
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.lang.Character.isDigit
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofLocalizedDateTime
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
        md.append(Heading(migration.gitlabProject.uppercase().replace("/".toRegex(), ""), 1))
            .emptyLine()
            .append(" migrated from ${migration.svnUrl}${migration.svnGroup}/${migration.svnProject} to ${migration.gitlabUrl}/${migration.gitlabGroup}")
            .emptyLine()
            .append(" gitlab user ${BoldText(ItalicText(if (migration.gitlabToken == null) applicationProperties.gitlab.account.uppercase() else migration.user.uppercase()))} on ${migration.date}")
            .emptyLine()
            .append(" subversion user ${BoldText(ItalicText(migration.svnUser.uppercase()))} on ${migration.date}")
            .emptyLine()
        if (migration.mappings.isNotEmpty()) {
            // Mapping
            md.append(Heading("Mappings applied", 3)).emptyLine()
            val mappingTableBuilder = Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
                .addRow("SVN directory", "Regex", "GIT directory", "Not Migrated from SVN")
            migration.mappings.forEach(Consumer { map: Mapping -> mappingTableBuilder.addRow(map.svnDirectory, map.regex, map.gitDirectory, map.isSvnDirectoryDelete) })
            md.append(mappingTableBuilder.build()).emptyLine()
        }
        if (!isEmpty(migration.forbiddenFileExtensions) ||
            !isEmpty(migration.maxFileSize) && isDigit(migration.maxFileSize[0])) {
            // Cleaning
            md.append(Heading("Cleaning options", 3)).emptyLine()
            val cleaningTableBuilder = Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                .addRow("Option", "Value")
            if (!isEmpty(migration.maxFileSize)) {
                cleaningTableBuilder.addRow("Max file size", migration.maxFileSize)
            }
            if (!isEmpty(migration.forbiddenFileExtensions) && isDigit(migration.maxFileSize[0])) {
                cleaningTableBuilder.addRow("Files extension(s) removed", migration.forbiddenFileExtensions)
            }
            if (!isEmpty(migration.svnHistory)) {
                cleaningTableBuilder.addRow("Subversion History", migration.svnHistory)
            }
            if (!isEmpty(migration.branchesToMigrate)) {
                cleaningTableBuilder.addRow("Branches to migrate", migration.branchesToMigrate)
            }
            if (!isEmpty(migration.tagsToMigrate)) {
                cleaningTableBuilder.addRow("Tags to migrate", migration.tagsToMigrate)
            }
            md.append(cleaningTableBuilder.build()).emptyLine()

            // *********** SVN Location File Size Report ******************
            md.append(Heading(
                "File Size Totals: SVN (${
                    humanReadableByteCount(cleanedFilesManager.fileSizeTotalBeforeClean, false)
                }), Gitlab (${humanReadableByteCount(cleanedFilesManager.fileSizeTotalAfterClean, false)})", 3))
                .emptyLine()
            val totalFileSizeTableBuilder = Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT, Table.ALIGN_RIGHT)
                .addRow("SVN Location", "Number of Files SVN", "Total File Size SVN", "Number of Files Gitlab", "Total File Size Gitlab")
            cleanedFilesManager.cleanedReportMap.forEach { (_: String?, value: CleanedFiles) ->
                totalFileSizeTableBuilder.addRow(
                    value.svnLocation,
                    value.fileCountBeforeClean,
                    humanReadableByteCount(value.fileSizeTotalBeforeClean, false),
                    value.fileCountAfterClean,
                    humanReadableByteCount(value.fileSizeTotalAfterClean, false)
                )
            }
            md.append(totalFileSizeTableBuilder.build()).emptyLine()

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : Extension Reason
            val migrationRemovedFilesReasonExtension = migrationRemovedFileService.findAllForMigrationAndReason(migration.id, Reason.EXTENSION)
            log.debug("0:migrationRemovedFilesReasonExtension.size():${migrationRemovedFilesReasonExtension.size}")
            if (migrationRemovedFilesReasonExtension.isNotEmpty()) {
                md.append(Heading("Migration Removed Files : Reason EXTENSION", 3)).emptyLine()
                val migrationRemovedFileReasonExtensionTableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "fileSize")
                migrationRemovedFilesReasonExtension.forEach(Consumer { migrationRemovedFile: MigrationRemovedFile -> addRemovedFileRow(migrationRemovedFileReasonExtensionTableBuilder, migrationRemovedFile) })
                md.append(migrationRemovedFileReasonExtensionTableBuilder.build()).emptyLine()
            }

            // *********** Files Removed ******************

            // Get MigrationRemovedFiles : SIZE Reason
            val migrationRemovedFilesReasonSize = migrationRemovedFileService.findAllForMigrationAndReason(migration.id, Reason.SIZE)
            log.debug("1:migrationRemovedFilesReasonSize.size():${migrationRemovedFilesReasonSize.size}")
            if (migrationRemovedFilesReasonSize.isNotEmpty()) {
                md.append(Heading("Migration Removed Files : Reason SIZE", 3)).emptyLine()
                val migrationRemovedFileReasonSizeTableBuilder = Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                    .addRow("Id", "Path", "SvnLocation", "Artifactory", "fileSize")
                migrationRemovedFilesReasonSize.forEach(Consumer { migrationRemovedFile: MigrationRemovedFile -> addRemovedFileRow(migrationRemovedFileReasonSizeTableBuilder, migrationRemovedFile) })
                md.append(migrationRemovedFileReasonSizeTableBuilder.build()).emptyLine()
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
        val formatter = ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.FRANCE)
            .withZone(ZoneId.systemDefault())
        md.append(Heading("Migration steps history (duration: $minutesDifference minutes)", 3)).emptyLine()
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
            else -> { "" }
        }
    }

    /**
     * Add a row with removed file information
     */
    open fun addRemovedFileRow(migrationRemovedFileTableBuilder: Table.Builder, migrationRemovedFile: MigrationRemovedFile) {
        var id = EMPTY
        if (migrationRemovedFile.id != null) {
            id = migrationRemovedFile.id.toString()
        }
        var path = EMPTY
        if (!isEmpty(migrationRemovedFile.path)) {
            path = migrationRemovedFile.path
        }
        var svnLocation = EMPTY
        if (!isEmpty(migrationRemovedFile.svnLocation)) {
            svnLocation = migrationRemovedFile.svnLocation
        }
        var fileSize: String? = EMPTY
        if (migrationRemovedFile.fileSize != null) {
            fileSize = humanReadableByteCount(migrationRemovedFile.fileSize, false)
        }
        migrationRemovedFileTableBuilder.addRow(id,
            path, svnLocation, "Yes", fileSize)
    }
}

fun StringBuilder.emptyLine(): java.lang.StringBuilder = this.append("\n\n")
