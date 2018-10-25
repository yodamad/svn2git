package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.Migration;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the Migration entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MigrationRepository extends JpaRepository<Migration, Long> {

}
