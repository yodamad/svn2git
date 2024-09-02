package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.isFileInFolder
import fr.yodamad.svn2git.functions.listTagsOnly
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.service.GitManager
import fr.yodamad.svn2git.service.HistoryManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException

@Service
open class GitTagManager(val gitManager: GitManager,
                         private val gitCommandManager: GitCommandManager,
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
        listTagsOnly(remotes)?.stream()?.filter {
            t -> workUnit.migration.tagsToMigrate == null || workUnit.migration.tagsToMigrate.split(",").any { a -> t.endsWith(a) }
        }?.forEach { t: String ->
            val warn: Boolean = pushTag(workUnit, t)
            gitCommandManager.sleepBeforePush(workUnit, warn)
        }
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
            LOG.debug("Tag $tagName")

            // determine noHistory flag i.e was all selected or not
            val noHistory = workUnit.migration.svnHistory != "all"

            // checkout a new branch using local tagName and remote tag name
            execCommand(workUnit.commandManager, workUnit.directory, "git checkout -f -b tmp_tag \"$tag\"")

            // If this tag does not contain any files we will ignore it and add warning to logs.
            if (!isFileInFolder(workUnit.directory)) {
                // Switch over to master
                execCommand(workUnit.commandManager, workUnit.directory, "git checkout -f master")

                // Now we can delete the branch tmp_tag
                execCommand(workUnit.commandManager, workUnit.directory, "git branch -D tmp_tag")
                historyMgr.endStep(history, StatusEnum.IGNORED, "Ignoring Tag: $tag : Because there are no files to commit.")
            } else {

                // creates a temporary orphan branch and renames it to tmp_tag
                if (noHistory) {
                    gitManager.removeHistory(workUnit, "tmp_tag", true, history)
                }

                // Checkout master.
                execCommand(workUnit.commandManager, workUnit.directory, "git checkout -f master")

                // create tag from tmp_tag branch.
                execCommand(workUnit.commandManager, workUnit.directory, "git tag \"$tagName\" tmp_tag")

                // add remote to master
                gitManager.addRemote(workUnit, false)

                // push the tag to remote
                // crashes if branch with same name so prefixing with refs/tags/
                execCommand(workUnit.commandManager, workUnit.directory, "git push -u origin \"refs/tags/$tagName\"")

                // delete the tmp_tag branch now that the tag has been created.
                execCommand(workUnit.commandManager, workUnit.directory, "git branch -D tmp_tag")
                historyMgr.endStep(history, StatusEnum.DONE)
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
