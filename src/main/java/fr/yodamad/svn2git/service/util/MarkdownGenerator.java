package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.domain.Migration;
import net.steppschuh.markdowngenerator.table.Table;
import net.steppschuh.markdowngenerator.text.emphasis.BoldText;
import net.steppschuh.markdowngenerator.text.emphasis.ItalicText;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;

/**
 * Markdown generator
 */
public abstract class MarkdownGenerator {

    private static final String EMPTY_LINE = "\n\n";

    /**
     * Genereate content of README.md for summary
     * @param migration Migration done
     * @return markdown string
     */
    public static String generateSummaryReadme(Migration migration) {
        StringBuilder md = new StringBuilder();
        // Overview
        md.append(new Heading(migration.getGitlabProject().toUpperCase().replaceAll("/", ""), 1))
            .append(EMPTY_LINE)
            .append(format(" migrated from %s%s%s to %s%s",
                migration.getSvnUrl(), migration.getSvnGroup(), migration.getSvnProject(),
                migration.getGitlabUrl(), migration.getGitlabGroup()))
            .append(EMPTY_LINE)
            .append(format(" by %s on %s",
                new BoldText(new ItalicText(migration.getUser().toUpperCase())),
                migration.getDate()))
            .append(EMPTY_LINE);

        if (!migration.getMappings().isEmpty()) {
            // Mapping
            md.append(new Heading("Mappings applied", 3)).append(EMPTY_LINE);
            Table.Builder mappingTableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
                .addRow("SVN directory", "Regex", "GIT directory");
            migration.getMappings().stream().forEach(map -> mappingTableBuilder.addRow(map.getSvnDirectory(), map.getRegex(), map.getGitDirectory()));
            md.append(mappingTableBuilder.build()).append(EMPTY_LINE);
        }

        if (!StringUtils.isEmpty(migration.getForbiddenFileExtensions()) || !StringUtils.isEmpty(migration.getMaxFileSize())) {
            // Cleaning
            md.append(new Heading("Cleaning options", 3)).append(EMPTY_LINE);
            Table.Builder cleaningTableBuilder = new Table.Builder()
                .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_LEFT)
                .addRow("", "");

            if (!StringUtils.isEmpty(migration.getMaxFileSize())) {
                cleaningTableBuilder.addRow("Max file size", migration.getMaxFileSize());
            }
            if (!StringUtils.isEmpty(migration.getForbiddenFileExtensions())) {
                cleaningTableBuilder.addRow("Files extension(s) removed", migration.getForbiddenFileExtensions());
            }
            md.append(cleaningTableBuilder.build()).append(EMPTY_LINE);
        }

        // History
        md.append(new Heading("Migration steps history", 3)).append(EMPTY_LINE);
        Table.Builder historyTableBuilder = new Table.Builder()
            .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT)
            .addRow("Step", "Status", "Details");
        migration.getHistories().stream().forEach(h -> historyTableBuilder.addRow(h.getStep(), h.getStatus(), h.getData()));
        md.append(historyTableBuilder.build()).append(EMPTY_LINE);

        return md.toString();
    }
}

