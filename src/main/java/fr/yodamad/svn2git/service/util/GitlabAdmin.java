package fr.yodamad.svn2git.service.util;

import fr.yodamad.svn2git.config.ApplicationProperties;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.UserApi;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class GitlabAdmin {

    /**
     * Gitlab API wrapper.
     */
    private GitLabApi gitLabApi;

    public GitlabAdmin(ApplicationProperties applicationProperties) {
        gitLabApi = new GitLabApi(applicationProperties.gitlab.url, applicationProperties.gitlab.token);
        gitLabApi.setIgnoreCertificateErrors(true);
    }

    // Getter on api

    public GitLabApi api() {
        return this.gitLabApi;
    }

    // Convenience getters

    public GroupApi groupApi() {
        return this.gitLabApi.getGroupApi();
    }

    public UserApi userApi() {
        return this.gitLabApi.getUserApi();
    }

    public ProjectApi projectApi() {
        return this.gitLabApi.getProjectApi();
    }

    // Setter on GitLabApi only

    public void setGitLabApi(GitLabApi gitLabApi) {
        this.gitLabApi = gitLabApi;
    }

}
