package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import fr.yodamad.svn2git.domain.MigrationRemovedFile
import fr.yodamad.svn2git.service.MigrationRemovedFileService
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException
import fr.yodamad.svn2git.web.rest.util.HeaderUtil
import io.github.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

/**
 * REST controller for managing MigrationRemovedFile.
 */
@RestController
@RequestMapping("/api")
open class MigrationRemovedFileResource(val migrationRemovedFileService: MigrationRemovedFileService) {

    private val log = LoggerFactory.getLogger(MigrationRemovedFileResource::class.java)

    private val ENTITY_NAME = "migrationRemovedFile"

    /**
     * POST  /migration-removed-files : Create a new migrationRemovedFile.
     *
     * @param migrationRemovedFile the migrationRemovedFile to create
     * @return the ResponseEntity with status 201 (Created) and with body the new migrationRemovedFile, or with status 400 (Bad Request) if the migrationRemovedFile has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PostMapping("/migration-removed-files")
    @Throws(URISyntaxException::class)
    open fun createMigrationRemovedFile(@RequestBody migrationRemovedFile: MigrationRemovedFile): ResponseEntity<MigrationRemovedFile>? {
        log.debug("REST request to save MigrationRemovedFile : {}", migrationRemovedFile)
        if (migrationRemovedFile.id != null) {
            throw BadRequestAlertException("A new migrationRemovedFile cannot already have an ID", ENTITY_NAME, "idexists")
        }
        val result = migrationRemovedFileService.save(migrationRemovedFile)
        return ResponseEntity.created(URI("/api/migration-removed-files/" + result.id))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * PUT  /migration-removed-files : Updates an existing migrationRemovedFile.
     *
     * @param migrationRemovedFile the migrationRemovedFile to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated migrationRemovedFile,
     * or with status 400 (Bad Request) if the migrationRemovedFile is not valid,
     * or with status 500 (Internal Server Error) if the migrationRemovedFile couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PutMapping("/migration-removed-files")
    @Throws(URISyntaxException::class)
    open fun updateMigrationRemovedFile(@RequestBody migrationRemovedFile: MigrationRemovedFile): ResponseEntity<MigrationRemovedFile>? {
        log.debug("REST request to update MigrationRemovedFile : {}", migrationRemovedFile)
        if (migrationRemovedFile.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = migrationRemovedFileService.save(migrationRemovedFile)
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, migrationRemovedFile.id.toString()))
            .body(result)
    }

    /**
     * GET  /migration-removed-files : get all the migrationRemovedFiles.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of migrationRemovedFiles in body
     */
    @Timed
    @GetMapping("/migration-removed-files")
    open fun getAllMigrationRemovedFiles(): List<MigrationRemovedFile?>? {
        log.debug("REST request to get all MigrationRemovedFiles")
        return migrationRemovedFileService.findAll()
    }

    /**
     * GET  /migration-removed-files/:id : get the "id" migrationRemovedFile.
     *
     * @param id the id of the migrationRemovedFile to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the migrationRemovedFile, or with status 404 (Not Found)
     */
    @Timed
    @GetMapping("/migration-removed-files/{id}")
    open fun getMigrationRemovedFile(@PathVariable id: Long?): ResponseEntity<MigrationRemovedFile>? {
        log.debug("REST request to get MigrationRemovedFile : {}", id)
        val migrationRemovedFile = migrationRemovedFileService.findOne(id)
        return ResponseUtil.wrapOrNotFound(migrationRemovedFile)
    }

    /**
     * DELETE  /migration-removed-files/:id : delete the "id" migrationRemovedFile.
     *
     * @param id the id of the migrationRemovedFile to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @Timed
    @DeleteMapping("/migration-removed-files/{id}")
    open fun deleteMigrationRemovedFile(@PathVariable id: Long): ResponseEntity<Void?>? {
        log.debug("REST request to delete MigrationRemovedFile : {}", id)
        migrationRemovedFileService.delete(id)
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build()
    }
}
