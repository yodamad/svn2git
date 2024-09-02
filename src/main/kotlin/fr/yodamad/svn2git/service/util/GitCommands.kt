package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.data.WorkUnit
import fr.yodamad.svn2git.functions.encode
import fr.yodamad.svn2git.functions.gitFormat
import fr.yodamad.svn2git.io.Shell.execCommand

// Keywords
const val GIT_PUSH = "git push"
const val COMMIT = "commit"
const val CONFIG = "config"
const val RESET = "reset"
const val BRANCH = "branch"
const val CHECKOUT = "checkout"

/** Default branch.  */
const val MASTER = "master"

fun gitCommand(command: String, flags: String? = "", target: String? = "") = "git $command $flags $target"

// Branch management
fun deleteBranch(branch: String) = gitCommand(BRANCH, "-D", "\"$branch\"")
fun renameBranch(branch: String) = gitCommand(BRANCH, "-m", "\"$branch\"")

// Pull management
fun checkoutFromOrigin(branch: String) = gitCommand(CHECKOUT, "-b", "${branch.gitFormat()} refs/remotes/origin/${branch.encode()}")
fun checkout(branch: String = MASTER) = gitCommand(CHECKOUT, target = branch.encode())

// Push management
fun add(element: String) = gitCommand("add", target = element)
fun commit(message: String) = gitCommand(COMMIT, "-m", "\"$message\"")
fun commitAll(message: String) = gitCommand(COMMIT, "-am", "\"$message\"")
fun push(branch: String = MASTER) = "$GIT_PUSH --set-upstream origin $branch"

// Maintenance management
fun gc() = gitCommand("gc")
fun resetHard(branch: String = MASTER) = gitCommand(RESET, "--hard", "origin/${branch.encode()}")
fun resetHead() = gitCommand(RESET, "--hard", "HEAD")
fun gitClean(commandManager: CommandManager, workUnit: WorkUnit) {
    try {
        execCommand(commandManager, workUnit.directory, gitCommand("reflog expire", "--expire=now --all"))
    } catch (_: RuntimeException) { }
    try {
        execCommand(commandManager, workUnit.directory, gitCommand("gc", "--prune=now --aggressive"))
    } catch (_: RuntimeException) { }
    execCommand(commandManager, workUnit.directory, gitCommand("reset", target = "HEAD"))
    execCommand(commandManager, workUnit.directory, gitCommand("clean", "-fd"))
}

// Config
fun readConfig(key: String) = gitCommand(CONFIG, target = key)
fun setConfig(key: String, value: String = "") = gitCommand(CONFIG, target = "$key $value")
