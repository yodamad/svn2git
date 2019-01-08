package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.SvnInfo;
import fr.yodamad.svn2git.domain.SvnStructure;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(SvnResource.class);
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

    /** SVN keywords. */
    private static final List<String> KEYWORDS = new ArrayList<>(3);
    static {
        KEYWORDS.add("trunk");
        KEYWORDS.add("branches");
        KEYWORDS.add("tags");
    }

    /**
     * Check if a SVN repository
     * @param repositoryName SVN repository ID search
     * @return if repository found
     */
    @PostMapping("repository/{repositoryName}")
    @Timed
    public ResponseEntity<SvnStructure> checkSVN(@PathVariable("repositoryName") String repositoryName, @RequestBody SvnInfo svnInfo) {
        SvnStructure structure = listSVN(svnInfo, repositoryName);

        // Repository not found case
        if (!structure.flat && structure.modules.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(structure);
    }

    /**
     * List directories in root folder of SVN repository
     * @param svnInfo SVN info to use
     * @param repo Repository to explore
     * @return list of directories found
     */
    private SvnStructure listSVN(SvnInfo svnInfo, String repo) {
        SvnStructure structure = new SvnStructure(repo);
        structure.modules = listModulesSVN(svnInfo, repo, null);
        structure.flat = structure.modules.isEmpty();
        log.info("SVN structure found : {}", structure);
        return structure;
    }

    /**
     * List modules into SVN structure
     * @param svnInfo Current svn info
     * @param repo Current svn repository
     * @param module Current module inspected
     * @return Complete module structure
     */
    private List<SvnStructure.SvnModule> listModulesSVN(SvnInfo svnInfo, String repo, SvnStructure.SvnModule module) {
        List<SvnStructure.SvnModule> modules = new ArrayList<>();
        log.info("Check for modules in {}", module);

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
            if (module == null) {
                list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(svnInfo.url + repo), revision));
            } else {
                list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(svnInfo.url + repo + module.path), revision));
            }
        } catch (SVNException e) {}

        list.setReceiver((target, object) -> {
            String name = object.getRelativePath();
            if (name != null && !name.isEmpty() && !KEYWORDS.contains(name)){
                if (module == null){
                    log.debug("Adding SVN module {}", name);
                    modules.add(new SvnStructure.SvnModule(name, ""));
                } else {
                    log.debug("Adding SVN submodule {} in {}", name, module);
                    modules.add(new SvnStructure.SvnModule(name, module.path));
                }
            }
        });
        try {
            list.run();
        } catch (SVNException ex) {}

        if (!modules.isEmpty()) {
            modules.forEach(svnSubMod -> svnSubMod.subModules.addAll(listModulesSVN(svnInfo, repo, svnSubMod)));
        }

        log.debug("SVN modules found in {} : {}", module, modules);
        return modules;
    }
}
