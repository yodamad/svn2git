package fr.yodamad.svn2git.config;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String DEFAULT_LANGUAGE = "en";

    // Constant String prepended to history steps data to indicated that step not really executed in
    // the context of a reexecution
    public static final String REEXECUTION_SKIPPING = "REEXECUTION_SKIPPING:" ;

    private Constants() {
    }
}
