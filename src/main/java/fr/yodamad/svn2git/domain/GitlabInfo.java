package fr.yodamad.svn2git.domain;

/**
 * Gitlab info to connect to API
 */
public class GitlabInfo {

    public String url;
    public String token;

    @Override
    public String toString() {
        return String.format("Gitlab connection to %s using token %s", url, token);
    }
}
