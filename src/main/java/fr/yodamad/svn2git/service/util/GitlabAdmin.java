package fr.yodamad.svn2git.service.util;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.UserApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitlabAdmin {

    /** Gitlab API wrapper. */
    private final GitLabApi gitLabApi;

    public GitlabAdmin(@Value("${gitlab.url}") String gitlabUrl, @Value("${gitlab.token}") String gitlabToken) {
        gitLabApi = new GitLabApi(gitlabUrl, gitlabToken);
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
}
