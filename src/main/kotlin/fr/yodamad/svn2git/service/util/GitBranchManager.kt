package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.functions.decode
import fr.yodamad.svn2git.functions.gitFormat
import fr.yodamad.svn2git.functions.listBranchesOnly
import fr.yodamad.svn2git.io.Shell.execCommand
import fr.yodamad.svn2git.service.GitManager
import fr.yodamad.svn2git.service.HistoryManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.function.Consumer

@Service
open class GitBranchManager(val gitManager: GitManager,
                            val historyMgr: HistoryManager,
                            val gitCommandManager: GitCommandManager,
                            val repoFormatter: GitRepositoryFormatter) {

    private val LOG = LoggerFactory.getLogger(GitBranchManager::class.java)
    private val FAILED_TO_PUSH_BRANCH = "Failed to push branch"

    /**
     * Push a branch
     *
     * @param workUnit
     * @param branch   Branch to migrate
     */
    @Throws(RuntimeException::class)
    open fun pushBranch(workUnit: WorkUnit, branch: String): Boolean {
        var branchName = branch.replaceFirst("refs/remotes/origin/".toRegex(), "")
        // Spaces aren't permitted, so replaced them with an underscore
        branchName = branchName.replaceFirst("origin/".toRegex(), "").gitFormat()
        LOG.debug("Branch $branchName")
        val history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, branchName)

        if (workUnit.migration.trunk != null && workUnit.migration.trunk != "trunk" && workUnit.migration.trunk.equals(branch.decode())) {
            // Don't push branch that is used as new master
            return true;
        }

        try {
            execCommand(workUnit.commandManager, workUnit.directory, "git checkout -f -b \"$branchName\" \"$branch\"")
        } catch (iEx: IOException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
            historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
            return false
        } catch (iEx: InterruptedException) {
            LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
            historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
            return false
        }
        if (workUnit.migration.svnHistory == "all") {
            try {
                gitManager.addRemote(workUnit, true)
                execCommand(workUnit.commandManager, workUnit.directory, "$GIT_PUSH --set-upstream origin \"$branchName\"")
                historyMgr.endStep(history, StatusEnum.DONE)
            } catch (iEx: IOException) {
                LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
                return false
            } catch (iEx: InterruptedException) {
                LOG.error(FAILED_TO_PUSH_BRANCH, iEx)
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.message)
                return false
            }
        } else {
            gitManager.removeHistory(workUnit, branchName, false, history)
        }
        return repoFormatter.applyMapping(workUnit, branch)
    }

    /**
     * Manage branches extracted from SVN
     *
     * @param workUnit
     * @param remotes
     */
    open fun manageBranches(workUnit: WorkUnit, remotes: List<String>) {
        listBranchesOnly(remotes, workUnit.migration.trunk)?.forEach(Consumer { b: String ->
            val warn: Boolean = pushBranch(workUnit, b)
            gitCommandManager.sleepBeforePush(workUnit, warn)
        })
    }
}
