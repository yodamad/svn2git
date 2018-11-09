package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.Mapping;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Mapping entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MappingRepository extends JpaRepository<Mapping, Long> {

}
