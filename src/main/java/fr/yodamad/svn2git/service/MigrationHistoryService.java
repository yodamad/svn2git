package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing MigrationHistory.
 */
@Service
@Transactional
public class MigrationHistoryService {

    private final Logger log = LoggerFactory.getLogger(MigrationHistoryService.class);

    private final MigrationHistoryRepository migrationHistoryRepository;

    public MigrationHistoryService(MigrationHistoryRepository migrationHistoryRepository) {
        this.migrationHistoryRepository = migrationHistoryRepository;
    }

    /**
     * Save a migrationHistory.
     *
     * @param migrationHistory the entity to save
     * @return the persisted entity
     */
    public MigrationHistory save(MigrationHistory migrationHistory) {
        log.debug("Request to save MigrationHistory : {}", migrationHistory);
        return migrationHistoryRepository.save(migrationHistory);
    }

    /**
     * Get all the migrationHistories.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<MigrationHistory> findAll() {
        log.debug("Request to get all MigrationHistories");
        return migrationHistoryRepository.findAll();
    }


    /**
     * Get one migrationHistory by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<MigrationHistory> findOne(Long id) {
        log.debug("Request to get MigrationHistory : {}", id);
        return migrationHistoryRepository.findById(id);
    }

    /**
     * Delete the migrationHistory by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete MigrationHistory : {}", id);
        migrationHistoryRepository.deleteById(id);
    }
}
