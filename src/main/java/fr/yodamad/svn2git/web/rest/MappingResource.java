package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.Mapping;
import fr.yodamad.svn2git.service.MappingService;
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
 * REST controller for managing Mapping.
 */
@RestController
@RequestMapping("/api")
public class MappingResource {

    private final Logger log = LoggerFactory.getLogger(MappingResource.class);

    private static final String ENTITY_NAME = "mapping";

    private final MappingService mappingService;

    public MappingResource(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    /**
     * POST  /mappings : Create a new mapping.
     *
     * @param mapping the mapping to create
     * @return the ResponseEntity with status 201 (Created) and with body the new mapping, or with status 400 (Bad Request) if the mapping has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/mappings")
    @Timed
    public ResponseEntity<Mapping> createMapping(@RequestBody Mapping mapping) throws URISyntaxException {
        log.debug("REST request to save Mapping : {}", mapping);
        if (mapping.getId() != null) {
            throw new BadRequestAlertException("A new mapping cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Mapping result = mappingService.save(mapping);
        return ResponseEntity.created(new URI("/api/mappings/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /mappings : Updates an existing mapping.
     *
     * @param mapping the mapping to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated mapping,
     * or with status 400 (Bad Request) if the mapping is not valid,
     * or with status 500 (Internal Server Error) if the mapping couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/mappings")
    @Timed
    public ResponseEntity<Mapping> updateMapping(@RequestBody Mapping mapping) throws URISyntaxException {
        log.debug("REST request to update Mapping : {}", mapping);
        if (mapping.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Mapping result = mappingService.save(mapping);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, mapping.getId().toString()))
            .body(result);
    }

    /**
     * GET  /mappings : get all the mappings.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of mappings in body
     */
    @GetMapping("/mappings")
    @Timed
    public List<Mapping> getAllMappings() {
        log.debug("REST request to get all Mappings");
        return mappingService.findAll();
    }

    /**
     * GET  /mappings/:id : get the "id" mapping.
     *
     * @param id the id of the mapping to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the mapping, or with status 404 (Not Found)
     */
    @GetMapping("/mappings/{id}")
    @Timed
    public ResponseEntity<Mapping> getMapping(@PathVariable Long id) {
        log.debug("REST request to get Mapping : {}", id);
        Optional<Mapping> mapping = mappingService.findOne(id);
        return ResponseUtil.wrapOrNotFound(mapping);
    }

    /**
     * DELETE  /mappings/:id : delete the "id" mapping.
     *
     * @param id the id of the mapping to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/mappings/{id}")
    @Timed
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        log.debug("REST request to delete Mapping : {}", id);
        mappingService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
