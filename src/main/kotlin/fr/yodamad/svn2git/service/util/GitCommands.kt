package fr.yodamad.svn2git.service.util

// Keywords
const val GIT_PUSH = "git push"
const val COMMIT = "commit"
const val CONFIG = "config"
const val RESET = "reset"

/** Default branch.  */
const val MASTER = "master"

private fun gitCommand(command: String, flags: String? = "", target: String? = "") = "git $command $flags $target"

// Branch management
fun deleteBranch(branch: String) = gitCommand("branch", "-D", branch)
fun renameBranch(branch: String) = gitCommand("branch", "-D", branch)

// Pull management
fun checkoutFromOrigin(branch: String) = "git checkout -b $branch refs/remotes/origin/$branch"
fun checkout(branch: String = MASTER) = "git checkout $branch"

// Push management
fun add(element: String) = gitCommand("add", target = element)
fun commit(message: String) = gitCommand(COMMIT, "-m", "\"$message\"")
fun commitAll(message: String) = gitCommand(COMMIT, "-am", "\"$message\"")
fun push(branch: String = MASTER) = "$GIT_PUSH --set-upstream origin $branch"

// Maintenance management
fun resetHard(branch: String = MASTER) = gitCommand(RESET, "--hard", "origin/$branch")
fun resetHead() = gitCommand(RESET, "--hard", "HEAD")

// Config
fun readConfig(key: String) = gitCommand(CONFIG, target = key)
fun setConfig(key: String, value: String = "") = gitCommand(CONFIG, target = "$key $value")
