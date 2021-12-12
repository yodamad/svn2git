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
    public Artifactory artifactory = new Artifactory();
    public Nexus nexus = new Nexus();
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

    public Nexus getNexus() { return nexus; }

    public void setNexus(Nexus nexus) { this.nexus = nexus; }

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

        public Boolean cleanAtTheEnd = false;

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

        public Boolean getCleanAtTheEnd() { return cleanAtTheEnd; }

        public void setCleanAtTheEnd(Boolean cleanAtTheEnd) { this.cleanAtTheEnd = cleanAtTheEnd; }
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

        /**
         * svnUrlModifiable
         */
        public String svnUrlModifiable;

        /**
         * Max attempts of git-svn fetch before failing migration
         */
        public Integer maxFetchAttempts;

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

        public String getSvnUrlModifiable() {
            return svnUrlModifiable;
        }

        public void setSvnUrlModifiable(String svnUrlModifiable) {
            this.svnUrlModifiable = svnUrlModifiable;
        }

        public Integer getMaxFetchAttempts() { return maxFetchAttempts; }

        public void setMaxFetchAttempts(Integer maxFetchAttempts) { this.maxFetchAttempts = maxFetchAttempts; }
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
        public Integer waitSeconds;
        /**
         * Dynamic Local Config
         */
        public List<String> dynamicLocalConfig;
        /**
         * Pause between push to gitlab
         */
        public long gitPushPauseMilliSeconds;
        /**
         * Enable package registry
         */
        public Boolean uploadToRegistry = false;

        public long getGitMvPauseMilliSeconds() {
            return gitMvPauseMilliSeconds;
        }

        public void setGitMvPauseMilliSeconds(long gitMvPauseMilliSeconds) {
            this.gitMvPauseMilliSeconds = gitMvPauseMilliSeconds;
        }

        /**
         * Pause between git mv operations
         */
        public long gitMvPauseMilliSeconds;

        public long getGitPushPauseMilliSeconds() {return gitPushPauseMilliSeconds;}

        public void setGitPushPauseMilliSeconds(long gitPushPauseMilliSeconds) {this.gitPushPauseMilliSeconds = gitPushPauseMilliSeconds;}

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

        public Integer getWaitSeconds() {
            return waitSeconds;
        }

        public void setWaitSeconds(Integer waitSeconds) {
            this.waitSeconds = waitSeconds;
        }

        public List<String> getDynamicLocalConfig() {
            return dynamicLocalConfig;
        }

        public void setDynamicLocalConfig(List<String> dynamicLocalConfig) { this.dynamicLocalConfig = dynamicLocalConfig; }

        public Boolean getUploadToRegistry() { return uploadToRegistry; }

        public void setUploadToRegistry(Boolean uploadToRegistry) { this.uploadToRegistry = uploadToRegistry; }
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
         * Artifactory accessToken.
         */
        public String accessToken;
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

        /*
         * To avoid overload of Artifactory a configurable pause between uploads.
         */
        public long uploadPauseMilliSeconds;

        public long getUploadPauseMilliSeconds() {return uploadPauseMilliSeconds;}

        public void setUploadPauseMilliSeconds(long uploadPauseSeconds) {this.uploadPauseMilliSeconds = uploadPauseSeconds;}

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

        public String getAccessToken() {return accessToken;}

        public void setAccessToken(String accessToken) {this.accessToken = accessToken;}
    }

    public static class Nexus {
        private String url;
        private String user;
        private String password;
        private String repository;
        private Boolean enabled;

        public String getUrl() { return url; }

        public void setUrl(String url) { this.url = url; }

        public String getUser() { return user; }

        public void setUser(String user) { this.user = user; }

        public String getPassword() { return password; }

        public void setPassword(String password) { this.password = password; }

        public String getRepository() { return repository; }

        public void setRepository(String repository) { this.repository = repository; }

        public Boolean getEnabled() { return enabled; }

        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
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

    public static class Flags {

        public boolean projectCleaningOption = false;

        public boolean gitlabGroupCreationOption = false;

        /**
         * add -f (force) option when executing git mv
         **/
        public boolean gitMvFOption = false;

        /**
         * add -k (skip) option when executing git mv
         **/
        public boolean gitMvKOption = false;

        /**
         * Cleanup Working Diretory when finished
         */
        public Boolean cleanupWorkDirectory;
        public boolean gitSvnClonePreserveEmptyDirsOption = false;

        /**
         * Added for printf %q issue on alpine
         */
        public boolean alpine = false;

        public Boolean getCleanupWorkDirectory() {
            return cleanupWorkDirectory;
        }

        public void setCleanupWorkDirectory(Boolean cleanupWorkDirectory) {
            this.cleanupWorkDirectory = cleanupWorkDirectory;
        }

        public boolean isGitSvnClonePreserveEmptyDirsOption() {
            return gitSvnClonePreserveEmptyDirsOption;
        }

        public void setGitSvnClonePreserveEmptyDirsOption(boolean gitSvnClonePreserveEmptyDirsOption) {
            this.gitSvnClonePreserveEmptyDirsOption = gitSvnClonePreserveEmptyDirsOption;
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

        public boolean isGitMvFOption() {
            return gitMvFOption;
        }

        public void setGitMvFOption(boolean gitMvFOption) {
            this.gitMvFOption = gitMvFOption;
        }

        public boolean isGitMvKOption() {
            return gitMvKOption;
        }

        public void setGitMvKOption(boolean gitMvKOption) {
            this.gitMvKOption = gitMvKOption;
        }

        public boolean isAlpine() {
            return alpine;
        }

        public void setAlpine(boolean alpine) {
            this.alpine = alpine;
        }
    }
}
