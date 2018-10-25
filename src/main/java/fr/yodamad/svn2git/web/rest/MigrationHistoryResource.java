package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.service.MigrationHistoryService;
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException;
import fr.yodamad.svn2git.web.rest.util.HeaderUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing MigrationHistory.
 */
@RestController
@RequestMapping("/api")
public class MigrationHistoryResource {

    private final Logger log = LoggerFactory.getLogger(MigrationHistoryResource.class);

    private static final String ENTITY_NAME = "migrationHistory";

    private final MigrationHistoryService migrationHistoryService;

    public MigrationHistoryResource(MigrationHistoryService migrationHistoryService) {
        this.migrationHistoryService = migrationHistoryService;
    }

    /**
     * POST  /migration-histories : Create a new migrationHistory.
     *
     * @param migrationHistory the migrationHistory to create
     * @return the ResponseEntity with status 201 (Created) and with body the new migrationHistory, or with status 400 (Bad Request) if the migrationHistory has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/migration-histories")
    @Timed
    public ResponseEntity<MigrationHistory> createMigrationHistory(@RequestBody MigrationHistory migrationHistory) throws URISyntaxException {
        log.debug("REST request to save MigrationHistory : {}", migrationHistory);
        if (migrationHistory.getId() != null) {
            throw new BadRequestAlertException("A new migrationHistory cannot already have an ID", ENTITY_NAME, "idexists");
        }
        MigrationHistory result = migrationHistoryService.save(migrationHistory);
        return ResponseEntity.created(new URI("/api/migration-histories/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
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
    @PutMapping("/migration-histories")
    @Timed
    public ResponseEntity<MigrationHistory> updateMigrationHistory(@RequestBody MigrationHistory migrationHistory) throws URISyntaxException {
        log.debug("REST request to update MigrationHistory : {}", migrationHistory);
        if (migrationHistory.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        MigrationHistory result = migrationHistoryService.save(migrationHistory);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, migrationHistory.getId().toString()))
            .body(result);
    }

    /**
     * GET  /migration-histories : get all the migrationHistories.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of migrationHistories in body
     */
    @GetMapping("/migration-histories")
    @Timed
    public List<MigrationHistory> getAllMigrationHistories() {
        log.debug("REST request to get all MigrationHistories");
        return migrationHistoryService.findAll();
    }

    /**
     * GET  /migration-histories/:id : get the "id" migrationHistory.
     *
     * @param id the id of the migrationHistory to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the migrationHistory, or with status 404 (Not Found)
     */
    @GetMapping("/migration-histories/{id}")
    @Timed
    public ResponseEntity<MigrationHistory> getMigrationHistory(@PathVariable Long id) {
        log.debug("REST request to get MigrationHistory : {}", id);
        Optional<MigrationHistory> migrationHistory = migrationHistoryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(migrationHistory);
    }

    /**
     * DELETE  /migration-histories/:id : delete the "id" migrationHistory.
     *
     * @param id the id of the migrationHistory to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/migration-histories/{id}")
    @Timed
    public ResponseEntity<Void> deleteMigrationHistory(@PathVariable Long id) {
        log.debug("REST request to delete MigrationHistory : {}", id);
        migrationHistoryService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
