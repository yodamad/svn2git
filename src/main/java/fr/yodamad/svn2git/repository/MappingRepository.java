package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Spring Data  repository for the Mapping entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MappingRepository extends JpaRepository<Mapping, Long> {

    List<Mapping> findAllByMigration(Long migrationId);
}
