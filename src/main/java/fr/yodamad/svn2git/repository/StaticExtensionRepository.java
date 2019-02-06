package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.StaticExtension;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the StaticExtension entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StaticExtensionRepository extends JpaRepository<StaticExtension, Long> {

}
