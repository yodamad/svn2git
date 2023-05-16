package fr.yodamad.svn2git.web.rest;

import com.codahale.metrics.annotation.Timed;
import fr.yodamad.svn2git.domain.StaticExtension;
import fr.yodamad.svn2git.repository.StaticExtensionRepository;
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException;
import fr.yodamad.svn2git.web.rest.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing StaticExtension.
 */
@RestController
@RequestMapping("/api")
public class StaticExtensionResource {

    private final Logger log = LoggerFactory.getLogger(StaticExtensionResource.class);

    private static final String ENTITY_NAME = "staticExtension";

    private final StaticExtensionRepository staticExtensionRepository;

    public StaticExtensionResource(StaticExtensionRepository staticExtensionRepository) {
        this.staticExtensionRepository = staticExtensionRepository;
    }

    /**
     * POST  /static-extensions : Create a new staticExtension.
     *
     * @param staticExtension the staticExtension to create
     * @return the ResponseEntity with status 201 (Created) and with body the new staticExtension, or with status 400 (Bad Request) if the staticExtension has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PostMapping("/static-extensions")
    @Timed
    public ResponseEntity<StaticExtension> createStaticExtension(@Valid @RequestBody StaticExtension staticExtension) throws URISyntaxException, BadRequestAlertException {
        log.debug("REST request to save StaticExtension : {}", staticExtension);
        if (staticExtension.getId() != null) {
            return ResponseEntity.badRequest().build();
        }
        StaticExtension result = staticExtensionRepository.save(staticExtension);
        return ResponseEntity.created(new URI("/api/static-extensions/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * PUT  /static-extensions : Updates an existing staticExtension.
     *
     * @param staticExtension the staticExtension to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated staticExtension,
     * or with status 400 (Bad Request) if the staticExtension is not valid,
     * or with status 500 (Internal Server Error) if the staticExtension couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @PutMapping("/static-extensions")
    @Timed
    public ResponseEntity<StaticExtension> updateStaticExtension(@Valid @RequestBody StaticExtension staticExtension) throws URISyntaxException, BadRequestAlertException {
        log.debug("REST request to update StaticExtension : {}", staticExtension);
        if (staticExtension.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        StaticExtension result = staticExtensionRepository.save(staticExtension);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, staticExtension.getId().toString()))
            .body(result);
    }

    /**
     * GET  /static-extensions : get all the staticExtensions.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of staticExtensions in body
     */
    @GetMapping("/static-extensions")
    @Timed
    public List<StaticExtension> getAllStaticExtensions() {
        log.debug("REST request to get all StaticExtensions");
        return staticExtensionRepository.findAll();
    }

    /**
     * GET  /static-extensions/:id : get the "id" staticExtension.
     *
     * @param id the id of the staticExtension to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the staticExtension, or with status 404 (Not Found)
     */
    @GetMapping("/static-extensions/{id}")
    @Timed
    public ResponseEntity<StaticExtension> getStaticExtension(@PathVariable Long id) {
        log.debug("REST request to get StaticExtension : {}", id);
        Optional<StaticExtension> staticExtension = staticExtensionRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(staticExtension);
    }

    /**
     * DELETE  /static-extensions/:id : delete the "id" staticExtension.
     *
     * @param id the id of the staticExtension to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/static-extensions/{id}")
    @Timed
    public ResponseEntity<Void> deleteStaticExtension(@PathVariable Long id) {
        log.debug("REST request to delete StaticExtension : {}", id);

        staticExtensionRepository.deleteById(id);
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build();
    }
}
