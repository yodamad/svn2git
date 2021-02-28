package fr.yodamad.svn2git.functions

val FULL_VERSION = "\\d+\\.\\d+\\.\\d+".toRegex()
val LITE_VERSION = "\\d+\\.\\d+".toRegex()

fun extractVersion(fullName: String): String {
    var version = FULL_VERSION.find(fullName)?.groupValues?.firstOrNull()
    if (version == null) {
        version = LITE_VERSION.find(fullName)?.groupValues?.firstOrNull()
        if (version == null) {
            version = "0.0.0"
        } else {
            version += ".0"
        }
    }
    return version
}
