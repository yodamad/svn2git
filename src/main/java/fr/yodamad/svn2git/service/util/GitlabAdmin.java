package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.config.ApplicationProperties;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.UserApi;
import org.springframework.stereotype.Component;

@Component
public class GitlabAdmin {

    /** Gitlab API wrapper. */
    private GitLabApi gitLabApi;

    public GitlabAdmin(ApplicationProperties applicationProperties) {
        gitLabApi = new GitLabApi(applicationProperties.gitlab.url, applicationProperties.gitlab.token);
        gitLabApi.setIgnoreCertificateErrors(true);
    }

    public GitLabApi api() {
        return this.gitLabApi;
    }

    public UserApi userApi() {
        return this.gitLabApi.getUserApi();
    }

    public GroupApi groupApi() {
        return this.gitLabApi.getGroupApi();
    }

    public ProjectApi projectApi() {
        return this.gitLabApi.getProjectApi();
    }

    public void setGitLabApi(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }
}
