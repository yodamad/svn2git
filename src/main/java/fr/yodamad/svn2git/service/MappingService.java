package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.Mapping;
import fr.yodamad.svn2git.repository.MappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing Mapping.
 */
@Service
@Transactional
public class MappingService {

    private final Logger log = LoggerFactory.getLogger(MappingService.class);

    private final MappingRepository mappingRepository;

    public MappingService(MappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    /**
     * Save a mapping.
     *
     * @param mapping the entity to save
     * @return the persisted entity
     */
    public Mapping save(Mapping mapping) {
        log.debug("Request to save Mapping : {}", mapping);
        return mappingRepository.save(mapping);
    }

    /**
     * Get all the mappings.
     *
     * @return the list of entities
     */
    @Transactional(readOnly = true)
    public List<Mapping> findAll() {
        log.debug("Request to get all Mappings");
        return mappingRepository.findAll();
    }


    /**
     * Get one mapping by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public Optional<Mapping> findOne(Long id) {
        log.debug("Request to get Mapping : {}", id);
        return mappingRepository.findById(id);
    }

    /**
     * Delete the mapping by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        log.debug("Request to delete Mapping : {}", id);
        mappingRepository.deleteById(id);
    }
}
