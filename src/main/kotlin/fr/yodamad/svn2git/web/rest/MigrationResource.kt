package fr.yodamad.svn2git.web.rest

import com.codahale.metrics.annotation.Timed
import com.fasterxml.jackson.annotation.JsonView
import fr.yodamad.svn2git.config.ApplicationProperties
import fr.yodamad.svn2git.domain.Mapping
import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.repository.MigrationRepository
import fr.yodamad.svn2git.service.MappingService
import fr.yodamad.svn2git.service.MigrationHistoryService
import fr.yodamad.svn2git.service.MigrationManager
import fr.yodamad.svn2git.web.rest.errors.BadRequestAlertException
import fr.yodamad.svn2git.web.rest.util.HeaderUtil
import fr.yodamad.svn2git.web.rest.util.PaginationUtil
import fr.yodamad.svn2git.web.rest.util.View.Public
import io.github.jhipster.web.util.ResponseUtil
import org.apache.commons.lang3.StringUtils
import org.gitlab4j.api.GitLabApiException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.net.URISyntaxException
import java.time.Instant
import java.time.LocalDate
import javax.validation.Valid

/**
 * REST controller for managing Migration.
 */
@RestController
@RequestMapping("$API$MIGRATIONS")
open class MigrationResource(val migrationRepository: MigrationRepository,
                             val migrationManager: MigrationManager,
                             val migrationHistoryService: MigrationHistoryService,
                             val mappingService: MappingService,
                             val gitlabResource: GitlabResource,
                             val applicationProperties: ApplicationProperties) {

    private val log = LoggerFactory.getLogger(MigrationResource::class.java)

    private val ENTITY_NAME = "migration"

    /**
     * POST  /migrations : Create a new migration.
     *
     * @param migration the migration to create
     * @return the ResponseEntity with status 201 (Created) and with body the new migration, or with status 400 (Bad Request) if the migration has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PostMapping
    @Throws(URISyntaxException::class, BadRequestAlertException::class)
    open fun createMigration(@RequestBody migration: @Valid Migration?): ResponseEntity<Migration?>? {
        log.debug("REST request to save Migration : {}", migration)
        if (migration!!.id != null) {
            throw BadRequestAlertException("A new migration cannot already have an ID", ENTITY_NAME, "idexists")
        }
        migration.date = LocalDate.now()
        migration.createdTimestamp = Instant.now()
        migration.status = StatusEnum.WAITING
        val result: Migration = init(migration)
        migrationManager.startMigration(result.id, false)
        return ResponseEntity.created(URI("/api/migrations/" + result.id))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.id.toString()))
            .body(migration)
    }

    /**
     * Retry a migration
     * @param id Migration ID to be retried
     * @return Migration initialized for retry
     * @throws URISyntaxException
     * @throws GitLabApiException
     */
    @Timed
    @PostMapping("/{id}/retry")
    @Throws(URISyntaxException::class, GitLabApiException::class)
    open fun retryMigraton(@PathVariable id: Long, @RequestBody forceClean: String?): ResponseEntity<Long>? {
        log.debug("REST request to retry Migration : {}", id)
        val mig = migrationRepository.findById(id).orElseThrow { BadRequestAlertException("Migration cannot be retried", ENTITY_NAME, "iddonotexist") }

        val clean = forceClean ?: "false";

        if (clean.toBoolean() && applicationProperties.flags.projectCleaningOption) {
            gitlabResource.removeGroup(mig)
        }
        log.debug("Create a new migration to retry")
        mig.id = null
        val result = init(mig)
        migrationManager.startMigration(result.id, true)
        return ResponseEntity.created(URI("/api/migrations/" + result.id))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.id.toString()))
            .body(id)
    }

    /**
     * Initialize a migration and its mappings
     * @param migration Migration to initialize
     * @return Migration initialized
     */
    open fun init(migration: Migration): Migration {
        val result = migrationRepository.save(migration)

        // Save any mapping where git directory is not empty Or is flagged for svnDirectoryDelete
        if (migration.mappings != null && migration.mappings.isNotEmpty()) {
            migration.mappings.stream()
                .filter { mp: Mapping -> !mp.isSvnDirectoryDelete && !StringUtils.isEmpty(mp.gitDirectory) || mp.isSvnDirectoryDelete }
                .forEach { mapping: Mapping ->
                    // Remove ID from static mapping
                    mapping.id = 0L
                    mapping.migration = result.id
                    mappingService.save(mapping)
                }
        }
        return result
    }

    /**
     * PUT  /migrations : Updates an existing migration.
     *
     * @param migration the migration to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated migration,
     * or with status 400 (Bad Request) if the migration is not valid,
     * or with status 500 (Internal Server Error) if the migration couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    @Timed
    @PutMapping
    @Throws(URISyntaxException::class)
    open fun updateMigration(@RequestBody migration: @Valid Migration?): ResponseEntity<Migration>? {
        log.debug("REST request to update Migration : {}", migration)
        if (migration!!.id == null) {
            throw BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull")
        }
        val result = migrationRepository.save(migration)
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, migration.id.toString()))
            .body(result)
    }

    /**
     * GET  /migrations : get all the migrations.
     *
     * @param pageable the pagination information
     * @return the ResponseEntity with status 200 (OK) and the list of migrations in body
     */
    @Timed
    @GetMapping
    @JsonView(Public::class)
    open fun getAllMigrations(pageable: Pageable?): ResponseEntity<List<Migration>>? {
        log.debug("REST request to get a page of Migrations")
        val page = migrationRepository.findAll(pageable)
        val headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/migrations")
        return ResponseEntity(page.content, headers, HttpStatus.OK)
    }

    /**
     * GET  /migrations/active : get all the migrations that are in running or waiting state
     *
     * @return the ResponseEntity with status 200 (OK) and the list of migrations in body
     */
    @Timed
    @GetMapping("/active")
    @JsonView(Public::class)
    open fun getAllActiveMigrations(): ResponseEntity<List<Migration>>? {
        log.debug("REST request to get a page of Migrations")
        val migrations = migrationRepository.findAllByStatusInOrderByDateDesc(listOf(StatusEnum.RUNNING, StatusEnum.WAITING))
        return ResponseEntity(migrations, null, HttpStatus.OK)
    }

    /**
     * GET  /migrations/user/:user : get all the migrations for a given user.
     *
     * @param user username for migrations
     * @return the ResponseEntity with status 200 (OK) and the list of migrations in body
     */
    @Timed
    @GetMapping("/user/{user}")
    @JsonView(Public::class)
    open fun getMigrationsByUser(@PathVariable user: String?): ResponseEntity<List<Migration>>? {
        log.debug("REST request to get a migrations of Migrations for a given user")
        val migrations = migrationRepository.findAllByUserIgnoreCaseOrderByIdDesc(user)
        return ResponseEntity(migrations, null, HttpStatus.OK)
    }

    /**
     * GET  /migrations/group/:group : get all the migrations for a given group.
     *
     * @param group group for migrations
     * @return the ResponseEntity with status 200 (OK) and the list of migrations in body
     */
    @Timed
    @GetMapping("/group/{group}")
    @JsonView(Public::class)
    open fun getMigrationsByGroup(@PathVariable group: String?): ResponseEntity<List<Migration>>? {
        log.debug("REST request to get a migrationRepositoryAllBySvnGroup of Migrations for a given group")
        val migrations = migrationRepository.findAllBySvnGroupIgnoreCaseOrderByIdDesc(group)
        return ResponseEntity(migrations, null, HttpStatus.OK)
    }

    /**
     * GET  /migrations/project/:group : get all the migrations for a given project.
     *
     * @param project project for migrations
     * @return the ResponseEntity with status 200 (OK) and the list of migrations in body
     */
    @Timed
    @GetMapping("/project/{project}")
    @JsonView(Public::class)
    open fun getMigrationsByProject(@PathVariable project: String?): ResponseEntity<List<Migration>>? {
        log.debug("REST request to get a migrations of Migrations for a given project")
        val migrations = migrationRepository.findAllBySvnProjectEndingWithIgnoreCaseOrderByIdDesc(project)
        return ResponseEntity(migrations, null, HttpStatus.OK)
    }

    /**
     * GET  /migrations/:id : get the "id" migration.
     *
     * @param id the id of the migration to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the migration, or with status 404 (Not Found)
     */
    @Timed
    @GetMapping("/{id}")
    @JsonView(Public::class)
    open fun getMigration(@PathVariable id: Long?): ResponseEntity<Migration>? {
        log.debug("REST request to get Migration : {}", id)
        val migration = migrationRepository.findById(id)
        return ResponseUtil.wrapOrNotFound(migration)
    }

    /**
     * GET  /migrations/:id/histories : get the histories linked to "id" migration.
     *
     * @param id the id of the migration to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the histories array, or with status 404 (Not Found)
     */
    @Timed
    @GetMapping("/{id}/histories")
    @JsonView(Public::class)
    open fun getMigrationHistories(@PathVariable id: Long?): ResponseEntity<List<MigrationHistory>>? {
        log.debug("REST request to get Migration : {}", id)
        val histories = migrationHistoryService.findAllForMigration(id)
        return ResponseEntity(histories, null, HttpStatus.OK)
    }

    /**
     * GET  /migrations/:id/mappings : get the mappings linked to "id" migration.
     *
     * @param id the id of the migration to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the mappings array, or with status 404 (Not Found)
     */
    @Timed
    @GetMapping("/{id}/mappings")
    open fun getMigrationMappings(@PathVariable id: Long?): ResponseEntity<List<Mapping>>? {
        log.debug("REST request to get Migration : {}", id)
        val mappings = mappingService.findAllForMigration(id)
        return ResponseEntity(mappings, null, HttpStatus.OK)
    }

    /**
     * DELETE  /migrations/:id : delete the "id" migration.
     *
     * @param id the id of the migration to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    @Timed
    @DeleteMapping("/{id}")
    open fun deleteMigration(@PathVariable id: Long): ResponseEntity<Void?>? {
        log.debug("REST request to delete Migration : {}", id)
        migrationRepository.deleteById(id)
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id.toString())).build()
    }
}
