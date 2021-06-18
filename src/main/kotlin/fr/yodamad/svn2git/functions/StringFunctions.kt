package fr.yodamad.svn2git.functions

import fr.yodamad.svn2git.io.Shell.isWindows
import org.apache.commons.lang3.StringUtils
import org.springframework.web.util.UriUtils.decode
import org.springframework.web.util.UriUtils.encode

val EMPTY = ""

fun formattedOrEmpty(element: String?, container: String, windowsCase: String? = null) =
    when {
        StringUtils.isEmpty(element) -> EMPTY
        windowsCase != null && isWindows -> String.format(container, element)
        else -> String.format(container, element)
    }

fun String.encode(): String = encode(this, "UTF-8")
fun String.decode(): String = decode(this, "UTF-8")
fun String.gitFormat(): String = this.decode().replace(" ", "_")

fun String.escape(): String = if (isWindows) this else this.replace("\\", "\\\\").replace("$", """\$""")
