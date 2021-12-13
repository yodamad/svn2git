package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.service.MigrationHistoryService
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException
import fr.yodamad.svn2git.web.rest.util.HeaderUtil
import tech.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

/**
 * REST controller for managing MigrationHistory.
 */
@RestController
@RequestMapping("$API$HISTORY")
open class MigrationHistoryResource(val migrationHistoryService: MigrationHistoryService) {
    private val log = LoggerFactory.getLogger(MigrationHistoryResource::class.java)

    private val ENTITY_NAME = "migrationHistory"

    /**
     * POST  /migration-histories : Create a new migrationHistory.
     *
     * @param migrationHistory the migrationHistory to create
     * @return the ResponseEntity with status 201 (Created) and with body the new migrationHistory, or with status 400 (Bad Request) if the migrationHistory has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PostMapping
    @Throws(URISyntaxException::class)
    open fun createMigrationHistory(@RequestBody migrationHistory: MigrationHistory): ResponseEntity<MigrationHistory>? {
        log.debug("REST request to save MigrationHistory : {}", migrationHistory)
        if (migrationHistory.id != null) {
            throw BadRequestAlertException("A new migrationHistory cannot already have an ID", ENTITY_NAME, "idexists")
        }
        val result = migrationHistoryService.save(migrationHistory)
        return ResponseEntity.created(URI("/api/migration-histories/" + result.id))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.id.toString()))
            .body(result)
    }

    /**
     * PUT  /migration-histories : Updates an existing migrationHistory.
     *
     * @param migrationHistory the migrationHistory to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated migrationHistory,
     * or with status 400 (Bad Request) if the migrationHistory is not valid,
     * or with status 500 (Internal Server Error) if the migrationHistory couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PutMapping
    @Throws(URISyntaxException::class)
    open fun updateMigrationHistory(@RequestBody migrationHistory: MigrationHistory): ResponseEntity<MigrationHistory>? {
        log.debug("REST request to update MigrationHistory : {}", migrationHistory)
        if (migrationHistory.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = migrationHistoryService.save(migrationHistory)
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, migrationHistory.id.toString()))
            .body(result)
    }

    /**
     * GET  /migration-histories : get all the migrationHistories.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of migrationHistories in body
     */
    @Timed
    @GetMapping
    open fun getAllMigrationHistories(): List<MigrationHistory?>? = migrationHistoryService.findAll()

    /**
     * GET  /migration-histories/:id : get the "id" migrationHistory.
     *
     * @param id the id of the migrationHistory to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the migrationHistory, or with status 404 (Not Found)
     */
    @Timed
    @GetMapping("/{id}")
    open fun getMigrationHistory(@PathVariable id: Long?): ResponseEntity<MigrationHistory>? {
        log.debug("REST request to get MigrationHistory : {}", id)
        val migrationHistory = migrationHistoryService.findOne(id)
        return ResponseUtil.wrapOrNotFound(migrationHistory)
    }

    /**
     * DELETE  /migration-histories/:id : delete the "id" migrationHistory.
     *
     * @param id the id of the migrationHistory to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @Timed
    @DeleteMapping("/{id}")
    open fun deleteMigrationHistory(@PathVariable id: Long): ResponseEntity<Void?>? {
        log.debug("REST request to delete MigrationHistory : {}", id)
        migrationHistoryService.delete(id)
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build()
    }
}
