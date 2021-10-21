package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.domain.SvnInfo
import fr.yodamad.svn2git.domain.SvnStructure
import fr.yodamad.svn2git.domain.SvnStructure.FakeModule
import fr.yodamad.svn2git.domain.SvnStructure.SvnModule
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.util.StringUtils.isEmpty
import org.springframework.web.bind.annotation.*
import org.tmatesoft.svn.core.*
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc2.ISvnObjectReceiver
import org.tmatesoft.svn.core.wc2.SvnOperationFactory
import org.tmatesoft.svn.core.wc2.SvnTarget
import java.util.function.Consumer

fun keywords() : MutableList<String> {
    val mutableList: MutableList<String> = ArrayList(3)
    mutableList.add("trunk")
    mutableList.add("branches")
    mutableList.add("tags")
    return mutableList;
}

@RestController
@RequestMapping("$API$SVN")
open class SvnResource(val applicationProperties: ApplicationProperties) {

    /** Logger.  */
    private val log = LoggerFactory.getLogger(SvnResource::class.java)

    /**
     * Check if a SVN repository
     * @param repositoryName SVN repository ID search
     * @return if repository found
     */
    @Timed
    @PostMapping("/repository/{repositoryName}")
    open fun checkSVN(
        @PathVariable("repositoryName") repositoryName: String?,
        @RequestParam("depth") depth: Int?,
        @RequestBody svnInfo: SvnInfo): ResponseEntity<SvnStructure?>? {
        val structure: SvnStructure = listSVN(svnInfo, repositoryName, depth)

        // Repository not found case
        return if (!structure.flat && structure.modules.isEmpty()) {
            ResponseEntity.notFound().build()
        } else ResponseEntity.ok().body(structure)
    }

    /**
     * List directories in root folder of SVN repository
     * @param svnInfo SVN info to use
     * @param repo Repository to explore
     * @return list of directories found
     */
    protected open fun listSVN(svnInfo: SvnInfo, repo: String?, depth: Int?): SvnStructure {
        val depthOrDefault = depth ?: 1
        val structure = SvnStructure(repo)
        structure.modules = listModulesSVN(svnInfo, repo, null, 0, depthOrDefault)
        if (structure.modules.stream().anyMatch { m: SvnModule? -> m is FakeModule }) {
            structure.root = true
            structure.modules.clear()
        }
        structure.flat = structure.modules.isEmpty()
        log.info("SVN structure found : {}", structure)
        return structure
    }

    /**
     * List modules into SVN structure
     * @param svnInfo Current svn info
     * @param repo Current svn repository
     * @param module Current module inspected
     * @return Complete module structure
     */
    protected open fun listModulesSVN(
        svnInfo: SvnInfo, repo: String?, module: SvnModule?, level: Int, maxDepth: Int): List<SvnModule>? {
        if (level == maxDepth) {
            log.info("Reaching max levels authorized for discovery, stop here")
            return emptyList()
        }
        val modules: MutableList<SvnModule> = ArrayList()
        if (module == null) {
            log.info("Check for modules in {}", String.format("%s/%s", svnInfo.url, repo))
        } else {
            log.info("Check for modules in {}", module)
        }
        val revision = SVNRevision.HEAD
        val operationFactory = SvnOperationFactory()

        // Set authentication if needed
        var authManager: ISVNAuthenticationManager? = DefaultSVNAuthenticationManager(null, true, applicationProperties.svn.user, applicationProperties.svn.password, null, null)
        if (!isEmpty(svnInfo.password)) {
            authManager = DefaultSVNAuthenticationManager(null, true, svnInfo.user, svnInfo.password, null, null)
        }

        operationFactory.authenticationManager = authManager
        val list = operationFactory.createList()
        list.depth = SVNDepth.IMMEDIATES
        list.revision = revision
        try {
            val svnUrl = if (svnInfo.url.endsWith("/")) svnInfo.url else String.format("%s/", svnInfo.url)
            if (module == null) {
                list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(String.format("%s%s", svnUrl, repo)), revision))
            } else {
                list.addTarget(SvnTarget.fromURL(SVNURL.parseURIEncoded(String.format("%s%s%s", svnUrl, repo, module.path)), revision))
            }
        } catch (e: SVNException) {
            log.error("Cannot list SVN", e)
            return emptyList()
        }
        val modulesFounds: MutableList<SvnModule> = ArrayList()
        list.receiver = ISvnObjectReceiver { _, `object`: SVNDirEntry ->
            val name = `object`.relativePath
            if (name != null && !name.isEmpty() && !keywords().contains(name) && module?.layoutElements.isNullOrEmpty()) {

                // found a directory
                if (`object`.kind == SVNNodeKind.DIR) {
                    if (module == null) {
                        log.debug("Adding SVN module {}", name)
                        modulesFounds.add(SvnModule(name, ""))
                    } else if (!module.flat) {
                        log.debug("Adding SVN submodule {} in {}", name, module)
                        modulesFounds.add(SvnModule(name, module.path))
                    }
                } else if (`object`.kind == SVNNodeKind.FILE && module != null) {
                    // file case : module may be flat, stop searching
                    module.flat = true
                    // remove potential folders previously found for this module
                    modulesFounds.clear()
                }
            }
            if (name != null && !name.isEmpty() && keywords().contains(name)) {
                if (module != null) {
                    log.info(String.format("Module %s with layout %s", module.name, name))
                    module.layoutElements.add(name)
                    module.flat = false
                    modulesFounds.clear()
                } else {
                    // Root level case
                    modulesFounds.add(FakeModule())
                }
            }
        }
        try {
            list.run()
        } catch (ex: SVNException) {
            if (ex is SVNAuthenticationException) {
                log.error("Cannot access SVN", ex)
                throw ex
            } else {
                log.warn("Flat repo", ex)
            }
        }
        modulesFounds.stream()
            .filter { m: SvnModule? -> m !is FakeModule }
            .map { e: SvnModule -> modules.add(e) }.count()
        if (modules.isNotEmpty()) {
            modules.forEach(
                Consumer { svnSubMod: SvnModule ->
                    svnSubMod.subModules.addAll(
                        listModulesSVN(svnInfo, repo, svnSubMod, level + 1, maxDepth)!!)
                })
        }
        if (modulesFounds.stream().anyMatch { m: SvnModule? -> m is FakeModule }) {
            return modulesFounds
        }
        log.debug("SVN modules found in {} : {}", module, modules)
        return modules
    }
}
