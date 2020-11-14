package fr.yodamad.svn2git.data

import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.service.util.CommandManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Work unit
 */
data class WorkUnit(var migration: Migration, var root: String, var directory: String, var warnings: AtomicBoolean, var commandManager: CommandManager)
