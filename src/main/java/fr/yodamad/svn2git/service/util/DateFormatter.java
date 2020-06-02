package fr.yodamad.svn2git.service.util;

import static java.lang.String.format;

public abstract class DateFormatter {

    private static final String MS = "ms";
    private static final String SEC = "s";
    private static final String MIN = "min";

    public static String toNiceFormat(Long milliseconds) {
        if (milliseconds < 1000) return format("%s%s", milliseconds, MS);

        Long seconds = milliseconds / 1000;
        Long ms = milliseconds % 1000;

        if (seconds < 60) return format("%s%s %s%s", seconds, SEC, ms == 0 ? "" : ms, ms == 0 ? "" : MS);

        Long minutes = seconds / 60;
        seconds = seconds % 60;

        return format("%s%s%s%s%s%s", minutes, MIN,
            seconds == 0 ? "" : format(" %s", seconds), seconds == 0 ? "" : SEC,
            ms == 0 ? "" : format(" %s", ms), ms == 0 ? "" : MS);
    }
}
