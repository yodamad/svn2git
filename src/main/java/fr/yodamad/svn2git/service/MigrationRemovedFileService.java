package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.repository.MigrationRemovedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service Implementation for managing MigrationRemovedFile.
 */
@Service
@Transactional
public class MigrationRemovedFileService {

    private final Logger log = LoggerFactory.getLogger(MigrationRemovedFileService.class);

    private final MigrationRemovedFileRepository migrationRemovedFileRepository;

    public MigrationRemovedFileService(MigrationRemovedFileRepository migrationRemovedFileRepository) {
        this.migrationRemovedFileRepository = migrationRemovedFileRepository;
    }

    /**
     * Save a migrationRemovedFile.
     *
     * @param migrationRemovedFile the entity to save
     * @return the persisted entity
     */
    public MigrationRemovedFile save(MigrationRemovedFile migrationRemovedFile) {
        log.debug("Request to save MigrationRemovedFile : {}", migrationRemovedFile);
        return migrationRemovedFileRepository.save(migrationRemovedFile);
    }

    /**
     * Get all the migrationRemovedFiles.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public Page<MigrationRemovedFile> findAll(Pageable pageable) {
        log.debug("Request to get all MigrationRemovedFiles");
        return migrationRemovedFileRepository.findAll(pageable);
    }


    /**
     * Get one migrationRemovedFile by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<MigrationRemovedFile> findOne(Long id) {
        log.debug("Request to get MigrationRemovedFile : {}", id);
        return migrationRemovedFileRepository.findById(id);
    }

    /**
     * Delete the migrationRemovedFile by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete MigrationRemovedFile : {}", id);        migrationRemovedFileRepository.deleteById(id);
    }
}
