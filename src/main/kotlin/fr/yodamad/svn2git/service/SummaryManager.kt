package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.service.util.*
import org.apache.commons.io.FileUtils
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.util.*

@Service
open class SummaryManager(val historyMgr: HistoryManager,
                          val markdownGenerator: MarkdownGenerator) {

    open fun prepareAndGenerate(commandManager: CommandManager, cleanedFilesManager: CleanedFilesManager, workUnit: WorkUnit, migration: Migration) {
        val history = historyMgr.startStep(migration, StepEnum.README_MD, "Generate README.md to summarize migration")
        try {
            execCommand(commandManager, workUnit.directory, checkout(MASTER))

            // If master not migrated, clean it to add only README.md
            if (migration.trunk == null) {
                Arrays.stream(File(workUnit.directory).listFiles())
                    .filter { f: File -> !f.name.equals(".git", ignoreCase = true) }
                    .forEach { f: File? ->
                        try {
                            FileUtils.forceDelete(f)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                execCommand(commandManager, workUnit.directory, commitAll("🧹 Clean master not migrated to add future REAMDE.md"))
            }
            historyMgr.endStep(history, StatusEnum.DONE)
            historyMgr.forceFlush()
            markdownGenerator.generateSummaryReadme(historyMgr.loadMigration(workUnit.migration.id), cleanedFilesManager, workUnit)
            execCommand(commandManager, workUnit.directory, add("README.md"))
            execCommand(commandManager, workUnit.directory, commit("📃 Add generated README.md"))
            execCommand(commandManager, workUnit.directory, push())
            historyMgr.endStep(history, StatusEnum.DONE)
        } catch (exc: Exception) {
            historyMgr.endStep(history, StatusEnum.FAILED, exc.message)
        }
    }
}
