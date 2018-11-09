package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.StaticMapping;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the StaticMapping entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StaticMappingRepository extends JpaRepository<StaticMapping, Long> {

}
