package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
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

    /**
     * @param svnUrl Configured SVN repository URL
     * @param svnUser SVN user to access repository
     * @param svnPassword SVN password to access repository
     */
    public SvnResource(@Value("${svn.url}") String svnUrl, @Value("${svn.user}") String svnUser, @Value("${svn.password}") String svnPassword) {
        this.svnUrl = svnUrl;
        /* ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( svnUser , svnPassword );
        repository.setAuthenticationManager( authManager ); */
    }

    /**
     * Check if a SVN repository
     * @param repositoryName SVN repository ID search
     * @return if repository found
     */
    @GetMapping("repository/{repositoryName}")
    @Timed
    public ResponseEntity<List<String>> checkSVN(@PathVariable("repositoryName") String repositoryName) {
        return ResponseEntity.ok()
                .body(listSVN(repositoryName));
    }

    /**
     * List directories in root folder of SVN repository
     * @param repo Repository to explore
     * @return list of directories found
     */
    private List<String> listSVN(String repo) {
        List<String> apiVersions = new ArrayList<>();
        SVNRevision revision = SVNRevision.HEAD;
        SvnOperationFactory operationFactory = new SvnOperationFactory();
        //operationFactory.setAuthenticationManager(new BasicAuthenticationManager(NAME, PASSWORD));
        SvnList list = operationFactory.createList();
        list.setDepth(SVNDepth.IMMEDIATES);
        list.setRevision(revision);
        try {
            list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(svnUrl + repo), revision));
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
