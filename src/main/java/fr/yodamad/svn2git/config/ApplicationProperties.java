package fr.yodamad.svn2git.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
    public Git git = new Git();
    public Artifactory artifactory = new Artifactory();
    public Password password = new Password();
    public Override override = new Override();
    public Flags flags = new Flags();

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

    public Artifactory getArtifactory() {
        return artifactory;
    }

    public void setArtifactory(Artifactory artifactory) {
        this.artifactory = artifactory;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
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

    public Flags getFlags() {
        return flags;
    }

    public void setFlags(Flags flags) {
        this.flags = flags;
    }

    public static class Work {

        public String directory = System.getenv("java.io.tmpdir");

        public Integer maxSvnLevel = 3;

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public Integer getMaxSvnLevel() {
            return maxSvnLevel;
        }

        public void setMaxSvnLevel(Integer maxSvnLevel) {
            this.maxSvnLevel = maxSvnLevel;
        }
    }

    public static class Svn {
        /**
         * SVN default user.
         */
        public String user;
        /**
         * SVN default password.
         */
        public String password;
        /**
         * SVN Url.
         */
        public String url;
        /**
         * Flag for credentials.
         */
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
        /**
         * Gitlab Url.
         */
        public String url;
        /**
         * Gitlab service account.
         */
        public String account;
        /**
         * Gitlab access token.
         */
        public String token;
        /**
         * Flag for credentials.
         */
        public String credentials;
        /**
         * Max waiting time.
         */
        public Integer wait;
        /**
         * Dynamic Local Config
         */
        public List<String> dynamicLocalConfig;

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

        public Integer getWait() {
            return wait;
        }

        public void setWait(Integer wait) {
            this.wait = wait;
        }

        public List<String> getDynamicLocalConfig() {
            return dynamicLocalConfig;
        }

        public void setDynamicLocalConfig(List<String> dynamicLocalConfig) {
            this.dynamicLocalConfig = dynamicLocalConfig;
        }
    }

    public static class Artifactory {
        /**
         * Artifactory support enablement flag.
         */
        public Boolean enabled;
        /**
         * Artifactory Url.
         */
        public String url;
        /**
         * Artifactory user.
         */
        public String user;
        /**
         * Artifactory password.
         */
        public String password;
        /**
         * Artifactory default repository.
         */
        public String repository;
        /**
         * Artifactory groupId prefix.
         */
        public String groupIdPrefix;

        /**
         * Used to delete contents of folder after pushing to artifactory
         * However bfg does not permit us to specify folder path (just a folder name)
         */
        public String deleteFolderWithBFG;

        /**
         * Binaries directory.
         */
        public String binariesDirectory;

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

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

        public String getRepository() {
            return repository;
        }

        public void setRepository(String repository) {
            this.repository = repository;
        }

        public String getGroupIdPrefix() {
            return groupIdPrefix;
        }

        public void setGroupIdPrefix(String groupIdPrefix) {
            this.groupIdPrefix = groupIdPrefix;
        }

        public String getBinariesDirectory() {
            return binariesDirectory;
        }

        public void setBinariesDirectory(String binariesDirectory) {
            this.binariesDirectory = binariesDirectory;
        }

        public String getDeleteFolderWithBFG() {return deleteFolderWithBFG;}

        public void setDeleteFolderWithBFG(String deleteFolderWithBFG) {this.deleteFolderWithBFG = deleteFolderWithBFG;}
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

    public static class Git {
        /**
         * add -f (force) option when executing git mv
         **/
        public boolean forceMvOnError = false;

        /**
         * add -k (skip) option when executing git mv
         **/
        public boolean skipMvOnError = false;
        public boolean preserveEmptyDirs = false;

        public boolean isPreserveEmptyDirs() {
            return preserveEmptyDirs;
        }

        public void setPreserveEmptyDirs(boolean preserveEmptyDirs) {
            this.preserveEmptyDirs = preserveEmptyDirs;
        }

        public boolean isForceMvOnError() {
            return forceMvOnError;
        }

        public void setForceMvOnError(boolean forceMvOnError) {
            this.forceMvOnError = forceMvOnError;
        }

        public boolean isSkipMvOnError() {
            return skipMvOnError;
        }

        public void setSkipMvOnError(boolean skipMvOnError) {
            this.skipMvOnError = skipMvOnError;
        }
    }

    public static class Flags {

        public boolean projectCleaningOption = false;

        public boolean gitlabGroupCreationOption = false;

        /**
         * Cleanup Working Diretory when finished
         */
        public Boolean cleanupWorkDirectory;

        public Boolean getCleanupWorkDirectory() {
            return cleanupWorkDirectory;
        }

        public void setCleanupWorkDirectory(Boolean cleanupWorkDirectory) {
            this.cleanupWorkDirectory = cleanupWorkDirectory;
        }

        public boolean isProjectCleaningOption() {
            return projectCleaningOption;
        }

        public void setProjectCleaningOption(boolean projectCleaningOption) {
            this.projectCleaningOption = projectCleaningOption;
        }

        public boolean isGitlabGroupCreationOption() {
            return gitlabGroupCreationOption;
        }

        public void setGitlabGroupCreationOption(boolean gitlabGroupCreationOption) {
            this.gitlabGroupCreationOption = gitlabGroupCreationOption;
        }
    }
}
