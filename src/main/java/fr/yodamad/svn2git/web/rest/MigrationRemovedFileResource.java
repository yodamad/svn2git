package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.service.MigrationRemovedFileService;
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
 * REST controller for managing MigrationRemovedFile.
 */
@RestController
@RequestMapping("/api")
public class MigrationRemovedFileResource {

    private final Logger log = LoggerFactory.getLogger(MigrationRemovedFileResource.class);

    private static final String ENTITY_NAME = "migrationRemovedFile";

    private final MigrationRemovedFileService migrationRemovedFileService;

    public MigrationRemovedFileResource(MigrationRemovedFileService migrationRemovedFileService) {
        this.migrationRemovedFileService = migrationRemovedFileService;
    }

    /**
     * POST  /migration-removed-files : Create a new migrationRemovedFile.
     *
     * @param migrationRemovedFile the migrationRemovedFile to create
     * @return the ResponseEntity with status 201 (Created) and with body the new migrationRemovedFile, or with status 400 (Bad Request) if the migrationRemovedFile has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/migration-removed-files")
    @Timed
    public ResponseEntity<MigrationRemovedFile> createMigrationRemovedFile(@RequestBody MigrationRemovedFile migrationRemovedFile) throws URISyntaxException {
        log.debug("REST request to save MigrationRemovedFile : {}", migrationRemovedFile);
        if (migrationRemovedFile.getId() != null) {
            throw new BadRequestAlertException("A new migrationRemovedFile cannot already have an ID", ENTITY_NAME, "idexists");
        }
        MigrationRemovedFile result = migrationRemovedFileService.save(migrationRemovedFile);
        return ResponseEntity.created(new URI("/api/migration-removed-files/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
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
    @PutMapping("/migration-removed-files")
    @Timed
    public ResponseEntity<MigrationRemovedFile> updateMigrationRemovedFile(@RequestBody MigrationRemovedFile migrationRemovedFile) throws URISyntaxException {
        log.debug("REST request to update MigrationRemovedFile : {}", migrationRemovedFile);
        if (migrationRemovedFile.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        MigrationRemovedFile result = migrationRemovedFileService.save(migrationRemovedFile);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, migrationRemovedFile.getId().toString()))
            .body(result);
    }

    /**
     * GET  /migration-removed-files : get all the migrationRemovedFiles.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of migrationRemovedFiles in body
     */
    @GetMapping("/migration-removed-files")
    @Timed
    public List<MigrationRemovedFile> getAllMigrationRemovedFiles() {
        log.debug("REST request to get all MigrationRemovedFiles");
        return migrationRemovedFileService.findAll();
    }

    /**
     * GET  /migration-removed-files/:id : get the "id" migrationRemovedFile.
     *
     * @param id the id of the migrationRemovedFile to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the migrationRemovedFile, or with status 404 (Not Found)
     */
    @GetMapping("/migration-removed-files/{id}")
    @Timed
    public ResponseEntity<MigrationRemovedFile> getMigrationRemovedFile(@PathVariable Long id) {
        log.debug("REST request to get MigrationRemovedFile : {}", id);
        Optional<MigrationRemovedFile> migrationRemovedFile = migrationRemovedFileService.findOne(id);
        return ResponseUtil.wrapOrNotFound(migrationRemovedFile);
    }

    /**
     * DELETE  /migration-removed-files/:id : delete the "id" migrationRemovedFile.
     *
     * @param id the id of the migrationRemovedFile to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/migration-removed-files/{id}")
    @Timed
    public ResponseEntity<Void> deleteMigrationRemovedFile(@PathVariable Long id) {
        log.debug("REST request to delete MigrationRemovedFile : {}", id);
        migrationRemovedFileService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
