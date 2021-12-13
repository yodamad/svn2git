package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.service.MappingService
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException
import fr.yodamad.svn2git.web.rest.util.HeaderUtil
import tech.jhipster.web.util.ResponseUtil
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException

/**
 * REST controller for managing Mapping.
 */
@RestController
@RequestMapping("$API$MAPPINGS")
open class MappingResource(val mappingService: MappingService) {

    private val log = LoggerFactory.getLogger(MappingResource::class.java)

    private val ENTITY_NAME = "mapping"

    /**
     * POST  /mappings : Create a new mapping.
     *
     * @param mapping the mapping to create
     * @return the ResponseEntity with status 201 (Created) and with body the new mapping, or with status 400 (Bad Request) if the mapping has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PostMapping
    @Throws(URISyntaxException::class)
    open fun createMapping(@RequestBody mapping: Mapping): ResponseEntity<Mapping>? {
        log.debug("REST request to save Mapping : {}", mapping)
        if (mapping.id != null) {
            throw BadRequestAlertException("A new mapping cannot already have an ID", ENTITY_NAME, "idexists")
        }
        val result = mappingService.save(mapping)
        return ResponseEntity.created(URI("/api/mappings/" + result.id))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.id.toString()))
            .body(result)
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
    @Timed
    @PutMapping
    @Throws(URISyntaxException::class)
    open fun updateMapping(@RequestBody mapping: Mapping): ResponseEntity<Mapping>? {
        log.debug("REST request to update Mapping : {}", mapping)
        if (mapping.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = mappingService.save(mapping)
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, mapping.id.toString()))
            .body(result)
    }

    /**
     * GET  /mappings : get all the mappings.
     *
     * @return the ResponseEntity with status 200 (OK) and the list of mappings in body
     */
    @Timed
    @GetMapping
    open fun getAllMappings(): List<Mapping?>? = mappingService.findAll()

    /**
     * GET  /mappings/:id : get the "id" mapping.
     *
     * @param id the id of the mapping to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the mapping, or with status 404 (Not Found)
     */
    @Timed
    @GetMapping("/{id}")
    open fun getMapping(@PathVariable id: Long?): ResponseEntity<Mapping>? {
        log.debug("REST request to get Mapping : {}", id)
        val mapping = mappingService.findOne(id)
        return ResponseUtil.wrapOrNotFound(mapping)
    }

    /**
     * DELETE  /mappings/:id : delete the "id" mapping.
     *
     * @param id the id of the mapping to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @Timed
    @DeleteMapping("/{id}")
    open fun deleteMapping(@PathVariable id: Long): ResponseEntity<Void?>? {
        log.debug("REST request to delete Mapping : {}", id)
        mappingService.delete(id)
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build()
    }
}
