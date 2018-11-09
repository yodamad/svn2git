package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.StaticMapping;
import fr.yodamad.svn2git.repository.StaticMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing StaticMapping.
 */
@Service
@Transactional
public class StaticMappingService {

    private final Logger log = LoggerFactory.getLogger(StaticMappingService.class);

    private final StaticMappingRepository staticMappingRepository;

    public StaticMappingService(StaticMappingRepository staticMappingRepository) {
        this.staticMappingRepository = staticMappingRepository;
    }

    /**
     * Save a staticMapping.
     *
     * @param staticMapping the entity to save
     * @return the persisted entity
     */
    public StaticMapping save(StaticMapping staticMapping) {
        log.debug("Request to save StaticMapping : {}", staticMapping);
        return staticMappingRepository.save(staticMapping);
    }

    /**
     * Get all the staticMappings.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<StaticMapping> findAll() {
        log.debug("Request to get all StaticMappings");
        return staticMappingRepository.findAll();
    }


    /**
     * Get one staticMapping by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<StaticMapping> findOne(Long id) {
        log.debug("Request to get StaticMapping : {}", id);
        return staticMappingRepository.findById(id);
    }

    /**
     * Delete the staticMapping by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete StaticMapping : {}", id);
        staticMappingRepository.deleteById(id);
    }
}
