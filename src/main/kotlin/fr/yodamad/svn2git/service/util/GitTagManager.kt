package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.isFileInFolder
import fr.yodamad.svn2git.functions.listTagsOnly
import fr.yodamad.svn2git.io.Shell
import fr.yodamad.svn2git.service.GitManager
import fr.yodamad.svn2git.service.HistoryManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.function.Consumer

@Service
open class GitTagManager(val gitManager: GitManager,
    val gitCommandManager: GitCommandManager,
    val historyMgr: HistoryManager) {

    private val LOG = LoggerFactory.getLogger(GitTagManager::class.java)
    private val FAILED_TO_PUSH_TAG = "Failed to push tag"

    /**
     * Manage tags extracted from SVN
     *
     * @param workUnit
     * @param remotes
     */
    open fun manageTags(workUnit: WorkUnit, remotes: List<String>) {
        listTagsOnly(remotes)?.forEach(Consumer { t: String ->
            val warn: Boolean = pushTag(workUnit, t)
            gitCommandManager.sleepBeforePush(workUnit, warn)
        })
    }

    /**
     * Push a tag
     *
     * @param workUnit Current work unit
     * @param tag      Tag to migrate
     */
    open fun pushTag(workUnit: WorkUnit, tag: String): Boolean {
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, tag)
        try {

            // derive local tagName from remote tag name
            val tagName = tag.replaceFirst(ORIGIN_TAGS.toRegex(), "")
            LOG.debug(String.format("Tag %s", tagName))

            // determine noHistory flag i.e was all selected or not
            val noHistory = workUnit.migration.svnHistory != "all"

            // checkout a new branch using local tagName and remote tag name
            var gitCommand = String.format("git checkout -b tmp_tag %s", tag)
            Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

            // If this tag does not contain any files we will ignore it and add warning to logs.
            if (!isFileInFolder(workUnit.directory)) {

                // Switch over to master
                gitCommand = "git checkout master"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // Now we can delete the branch tmp_tag
                gitCommand = "git branch -D tmp_tag"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.IGNORED, "Ignoring Tag: $tag : Because there are no files to commit.")
            } else {

                // creates a temporary orphan branch and renames it to tmp_tag
                if (noHistory) {
                    gitManager.removeHistory(workUnit, "tmp_tag", true, history)
                }

                // Checkout master.
                gitCommand = "git checkout master"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // create tag from tmp_tag branch.
                gitCommand = String.format("git tag %s tmp_tag", tagName)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // add remote to master
                gitManager.addRemote(workUnit, false)

                // push the tag to remote
                // crashes if branch with same name so prefixing with refs/tags/
                gitCommand = String.format("git push -u origin refs/tags/%s", tagName)
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)

                // delete the tmp_tag branch now that the tag has been created.
                gitCommand = "git branch -D tmp_tag"
                Shell.execCommand(workUnit.commandManager, workUnit.directory, gitCommand)
                historyMgr.endStep(history, StatusEnum.DONE, null)
            }
        } catch (gitEx: IOException) {
            LOG.error(FAILED_TO_PUSH_TAG, gitEx)
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        } catch (gitEx: InterruptedException) {
            LOG.error(FAILED_TO_PUSH_TAG, gitEx)
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.message)
        }
        return false
    }

}
