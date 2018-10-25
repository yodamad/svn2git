package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.MigrationHistory;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the MigrationHistory entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MigrationHistoryRepository extends JpaRepository<MigrationHistory, Long> {

}
