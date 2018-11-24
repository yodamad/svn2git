package fr.yodamad.svn2git.service.util;

/**
 * Migrations constants
 */
public abstract class MigrationConstants {
    /** Default ref origin for tags. */
    public static final String ORIGIN_TAGS = "origin/tags/";
    /** Temp directory. */
    public static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    /** Default branch. */
    public static final String MASTER = "master";
    /** Git push command. */
    public static final String GIT_PUSH = "git push";
    /** Stars to hide sensitive data. */
    public static final String STARS = "******";
    /** Execution error. */
    public static final int ERROR_CODE = 128;
}
