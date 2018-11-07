package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.MigrationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Spring Data  repository for the MigrationHistory entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MigrationHistoryRepository extends JpaRepository<MigrationHistory, Long> {

    List<MigrationHistory> findAllByMigration_Id(Long migrationId);
}
