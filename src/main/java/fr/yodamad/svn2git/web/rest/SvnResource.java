package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.SvnInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller to use SVN
 */
@RestController
@RequestMapping("/api/svn/")
public class SvnResource {

    /** SVN global url. */
    private final String svnUrl;
    /** SVN default user. */
    @Value("${svn.user}") String svnUser;
    /** SVN default password. */
    @Value("${svn.password}") String svnPassword;

    /**
     * @param svnUrl Configured SVN repository URL
     */
    public SvnResource(@Value("${svn.url}") String svnUrl) {
        this.svnUrl = svnUrl;
    }

    /**
     * Check if a SVN repository
     * @param repositoryName SVN repository ID search
     * @return if repository found
     */
    @PostMapping("repository/{repositoryName}")
    @Timed
    public ResponseEntity<List<String>> checkSVN(@PathVariable("repositoryName") String repositoryName, @RequestBody SvnInfo svnInfo) {
        return ResponseEntity.ok()
                .body(listSVN(svnInfo, repositoryName));
    }

    /**
     * List directories in root folder of SVN repository
     * @param svnInfo SVN info to use
     * @param repo Repository to explore
     * @return list of directories found
     */
    private List<String> listSVN(SvnInfo svnInfo, String repo) {
        List<String> apiVersions = new ArrayList<>();
        SVNRevision revision = SVNRevision.HEAD;
        SvnOperationFactory operationFactory = new SvnOperationFactory();

        // Set authentication if needed
        if (!StringUtils.isEmpty(svnInfo.user)) {
            operationFactory.setAuthenticationManager(BasicAuthenticationManager.newInstance(svnInfo.user, svnInfo.password.toCharArray()));
        } else if (!StringUtils.isEmpty(svnUser)) {
            operationFactory.setAuthenticationManager(BasicAuthenticationManager.newInstance(svnUser, svnPassword.toCharArray()));
        }

        SvnList list = operationFactory.createList();
        list.setDepth(SVNDepth.IMMEDIATES);
        list.setRevision(revision);
        try {
            list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(svnInfo.url + repo), revision));
        } catch (SVNException e) {
        }
        list.setReceiver((target, object) -> {
            String name = object.getRelativePath();
            if(name!=null && !name.isEmpty()){
                apiVersions.add(name);
            }
        });
        try {
            list.run();
        } catch (SVNException ex) {
        }

        return apiVersions;
    }
}
