package fr.yodamad.svn2git.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Svn 2 Git.
 * <p>
 * Properties are configured in the application.yml file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    public Work work = new Work();
    public Svn svn = new Svn();
    public Gitlab gitlab = new Gitlab();
    public Password password = new Password();
    public Override override = new Override();

    public static class Work {
        public String directory = System.getenv("java.io.tmpdir");

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }
    }

    public static class Svn {
        /** SVN default user. */
        public String user;
        /** SVN default password. */
        public String password;
        /** SVN Url. */
        public String url;
        /** Flag for credentials. */
        public String credentials;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCredentials() {
            return credentials;
        }

        public void setCredentials(String credentials) {
            this.credentials = credentials;
        }
    }

    public static class Gitlab {
        /** Gitlab Url. */
        public String url;
        /** Gitlab service account. */
        public String account;
        /** Gitlab access token. */
        public String token;
        /** Flag for credentials. */
        public String credentials;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getCredentials() {
            return credentials;
        }

        public void setCredentials(String credentials) {
            this.credentials = credentials;
        }
    }

    public static class Password {
        public String cipher;

        public String getCipher() {
            return cipher;
        }

        public void setCipher(String cipher) {
            this.cipher = cipher;
        }
    }

    public static class Override {
        public boolean extensions;
        public boolean mappings;

        public boolean isExtensions() {
            return extensions;
        }

        public void setExtensions(boolean extensions) {
            this.extensions = extensions;
        }

        public boolean isMappings() {
            return mappings;
        }

        public void setMappings(boolean mappings) {
            this.mappings = mappings;
        }
    }


    public Work getWork() {
        return work;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public Svn getSvn() {
        return svn;
    }

    public void setSvn(Svn svn) {
        this.svn = svn;
    }

    public Gitlab getGitlab() {
        return gitlab;
    }

    public void setGitlab(Gitlab gitlab) {
        this.gitlab = gitlab;
    }

    public Password getPassword() {
        return password;
    }

    public void setPassword(Password password) {
        this.password = password;
    }

    public Override getOverride() {
        return override;
    }

    public void setOverride(Override override) {
        this.override = override;
    }
}
