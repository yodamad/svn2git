package fr.yodamad.svn2git.service.util

import fr.yodamad.svn2git.config.ApplicationProperties
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GroupApi
import org.gitlab4j.api.ProjectApi
import org.gitlab4j.api.UserApi
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
open class GitlabAdmin(val applicationProperties: ApplicationProperties) {

    @PostConstruct
    open fun initApi() {
        gitLabApi = GitLabApi(applicationProperties.gitlab.url, applicationProperties.gitlab.token)
        gitLabApi.ignoreCertificateErrors = true
    }

    /**
     * Gitlab API wrapper.
     */
    lateinit var gitLabApi : GitLabApi

    // Getter on api
    open fun api(): GitLabApi {
        return gitLabApi
    }

    // Convenience getters
    open fun groupApi(): GroupApi {
        return gitLabApi.groupApi
    }

    open fun userApi(): UserApi {
        return gitLabApi.userApi
    }

    open fun projectApi(): ProjectApi {
        return gitLabApi.projectApi
    }

    open fun setGitlabApi(api: GitLabApi) {
        gitLabApi = api
    }

    /*init {
        gitLabApi = GitLabApi(applicationProperties.gitlab.url, applicationProperties.gitlab.token)
        gitLabApi.ignoreCertificateErrors = true
    }*/
}
