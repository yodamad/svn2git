package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.StaticMapping;
import fr.yodamad.svn2git.service.StaticMappingService;
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException;
import fr.yodamad.svn2git.web.rest.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing StaticMapping.
 */
@RestController
@RequestMapping("/api")
public class StaticMappingResource {

    private final Logger log = LoggerFactory.getLogger(StaticMappingResource.class);

    private static final String ENTITY_NAME = "staticMapping";

    private final StaticMappingService staticMappingService;

    public StaticMappingResource(StaticMappingService staticMappingService) {
        this.staticMappingService = staticMappingService;
    }

    /**
     * POST  /static-mappings : Create a new staticMapping.
     *
     * @param staticMapping the staticMapping to create
     * @return the ResponseEntity with status 201 (Created) and with body the new staticMapping, or with status 400 (Bad Request) if the staticMapping has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/static-mappings")
    @Timed
    public ResponseEntity<StaticMapping> createStaticMapping(@RequestBody StaticMapping staticMapping) throws URISyntaxException {
        log.debug("REST request to save StaticMapping : {}", staticMapping);
        if (staticMapping.getId() != null) {
            throw new BadRequestAlertException("A new staticMapping cannot already have an ID", ENTITY_NAME, "idexists");
        }
        StaticMapping result = staticMappingService.save(staticMapping);
        return ResponseEntity.created(new URI("/api/static-mappings/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /static-mappings : Updates an existing staticMapping.
     *
     * @param staticMapping the staticMapping to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated staticMapping,
     * or with status 400 (Bad Request) if the staticMapping is not valid,
     * or with status 500 (Internal Server Error) if the staticMapping couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/static-mappings")
    @Timed
    public ResponseEntity<StaticMapping> updateStaticMapping(@RequestBody StaticMapping staticMapping) throws URISyntaxException {
        log.debug("REST request to update StaticMapping : {}", staticMapping);
        if (staticMapping.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        StaticMapping result = staticMappingService.save(staticMapping);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, staticMapping.getId().toString()))
            .body(result);
    }

    /**
     * GET  /static-mappings : get all the staticMappings.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of staticMappings in body
     */
    @GetMapping("/static-mappings")
    @Timed
    public List<StaticMapping> getAllStaticMappings() {
        log.debug("REST request to get all StaticMappings");
        return staticMappingService.findAll();
    }

    /**
     * GET  /static-mappings/:id : get the "id" staticMapping.
     *
     * @param id the id of the staticMapping to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the staticMapping, or with status 404 (Not Found)
     */
    @GetMapping("/static-mappings/{id}")
    @Timed
    public ResponseEntity<StaticMapping> getStaticMapping(@PathVariable Long id) {
        log.debug("REST request to get StaticMapping : {}", id);
        Optional<StaticMapping> staticMapping = staticMappingService.findOne(id);
        return ResponseUtil.wrapOrNotFound(staticMapping);
    }

    /**
     * DELETE  /static-mappings/:id : delete the "id" staticMapping.
     *
     * @param id the id of the staticMapping to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/static-mappings/{id}")
    @Timed
    public ResponseEntity<Void> deleteStaticMapping(@PathVariable Long id) {
        log.debug("REST request to delete StaticMapping : {}", id);
        staticMappingService.delete(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
