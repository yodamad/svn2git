package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.SvnInfo;
import fr.yodamad.svn2git.domain.SvnStructure;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnList;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * Controller to use SVN
 */
@RestController
@RequestMapping("/api/svn/")
public class SvnResource {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(SvnResource.class);
    /** SVN global url. */
    private final ApplicationProperties applicationProperties;

    /**
     * @param applicationProperties Application properties
     */
    public SvnResource(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
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
    public ResponseEntity<SvnStructure> checkSVN(
        @PathVariable("repositoryName") String repositoryName,
        @RequestParam("depth") Integer depth,
        @RequestBody SvnInfo svnInfo) {
        SvnStructure structure = listSVN(svnInfo, repositoryName, depth);

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
    protected SvnStructure listSVN(SvnInfo svnInfo, String repo, Integer depth) {
        SvnStructure structure = new SvnStructure(repo);
        structure.modules = listModulesSVN(svnInfo, repo, null, 0, depth);
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
    protected List<SvnStructure.SvnModule> listModulesSVN(
        SvnInfo svnInfo, String repo, SvnStructure.SvnModule module, final int level, Integer maxDepth) {

        if (level == maxDepth) {
            log.info("Reaching max levels authorized for discovery, stop here");
            return Collections.emptyList();
        }

        List<SvnStructure.SvnModule> modules = new ArrayList<>();
        if (module == null) {
            log.info("Check for modules in {}", format("%s/%s", svnInfo.url, repo));
        } else {
            log.info("Check for modules in {}", module);
        }

        SVNRevision revision = SVNRevision.HEAD;
        SvnOperationFactory operationFactory = new SvnOperationFactory();

        // Set authentication if needed
        ISVNAuthenticationManager authManager = new DefaultSVNAuthenticationManager(null, true, svnInfo.user, svnInfo.password, null, null);
        if (!StringUtils.isEmpty(applicationProperties.svn.user)) {
            authManager = new DefaultSVNAuthenticationManager(null, true,
                applicationProperties.svn.user, applicationProperties.svn.password, null, null);
            operationFactory.setAuthenticationManager(authManager);
        }
        operationFactory.setAuthenticationManager(authManager);

        SvnList list = operationFactory.createList();
        list.setDepth(SVNDepth.IMMEDIATES);
        list.setRevision(revision);

        try {
            String svnUrl = svnInfo.url.endsWith("/") ? svnInfo.url : format("%s/", svnInfo.url);
            if (module == null) {
                list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(format("%s%s", svnUrl, repo)), revision));
            } else {
                list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(format("%s%s%s", svnUrl, repo, module.path)), revision));
            }
        } catch (SVNException e) {
            log.error("Cannot list SVN", e);
        }

        List<SvnStructure.SvnModule> modulesFounds = new ArrayList<>();

        list.setReceiver((target, object) -> {
            String name = object.getRelativePath();
            if (name != null && !name.isEmpty() && !KEYWORDS.contains(name)) {

                // found a directory
                if (object.getKind() == SVNNodeKind.DIR) {
                    if (module == null){
                        log.debug("Adding SVN module {}", name);
                        modulesFounds.add(new SvnStructure.SvnModule(name, ""));
                    } else if (!module.flat) {
                        log.debug("Adding SVN submodule {} in {}", name, module);
                        modulesFounds.add(new SvnStructure.SvnModule(name, module.path));
                    }
                } else if (object.getKind() == SVNNodeKind.FILE && module != null) {
                    // file case : module may be flat, stop searching
                    module.flat = true;
                    // remove potential folders previously found for this module
                    modulesFounds.clear();
                }
            }

            if (module != null && name != null && !name.isEmpty() && KEYWORDS.contains(name)) {
                log.info(format("Module %s with layout %s", module.name, name));
                module.layoutElements.add(name);
            }
        });

        try {
            list.run();
        } catch (SVNException ex) {
            log.error("Cannot list SVN", ex);
        }

        if (!modulesFounds.isEmpty()) {
            modules.addAll(modulesFounds);
        }

        if (!modules.isEmpty()) {
            modules.forEach(
                svnSubMod -> svnSubMod.subModules.addAll(
                    listModulesSVN(svnInfo, repo, svnSubMod, level + 1, maxDepth)));
        }

        log.debug("SVN modules found in {} : {}", module, modules);
        return modules;
    }
}
