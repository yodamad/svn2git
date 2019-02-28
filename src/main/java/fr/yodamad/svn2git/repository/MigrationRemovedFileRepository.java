package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;


/**
 * Spring Data  repository for the MigrationRemovedFile entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MigrationRemovedFileRepository extends JpaRepository<MigrationRemovedFile, Long> {

}
