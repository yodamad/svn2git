package fr.yodamad.svn2git.service.util

object DateFormatter {
    private const val MS = "ms"
    private const val SEC = "s"
    private const val MIN = "min"
    fun toNiceFormat(milliseconds: Long): String {
        if (milliseconds < 1000) return String.format("%s%s", milliseconds, MS)
        var seconds = milliseconds / 1000
        val ms = milliseconds % 1000
        if (seconds < 60) return String.format("%s%s %s%s", seconds, SEC, if (ms == 0L) "" else ms, if (ms == 0L) "" else MS)
        val minutes = seconds / 60
        seconds %= 60
        return String.format("%s%s%s%s%s%s", minutes, MIN,
            if (seconds == 0L) "" else String.format(" %s", seconds), if (seconds == 0L) "" else SEC,
            if (ms == 0L) "" else String.format(" %s", ms), if (ms == 0L) "" else MS)
    }
}
