package fr.yodamad.svn2git.functions

import fr.yodamad.svn2git.io.Shell.isWindows
import org.apache.commons.lang3.StringUtils

val EMPTY = ""

fun formattedOrEmpty(element: String?, container: String, windowsCase: String? = null) =
    when {
        StringUtils.isEmpty(element) -> EMPTY
        windowsCase != null && isWindows -> String.format(container, element)
        else -> String.format(container, element)
    }
