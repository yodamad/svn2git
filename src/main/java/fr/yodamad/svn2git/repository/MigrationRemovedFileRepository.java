package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data  repository for the MigrationRemovedFile entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MigrationRemovedFileRepository extends JpaRepository<MigrationRemovedFile, Long> {

    /**
     * Find all removed files for a migration
     * @param migrationId Migration ID
     * @return List of files
     */
    List<MigrationRemovedFile> findAllByMigration_Id(Long migrationId);

    /**
     * Count all removed files for a migration
     * @param migrationId Migration ID
     * @return Nb of files
     */
    Integer countAllByMigration_Id(Long migrationId);
}
