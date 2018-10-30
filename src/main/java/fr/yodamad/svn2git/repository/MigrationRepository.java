package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;


/**
 * Spring Data  repository for the Migration entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MigrationRepository extends JpaRepository<Migration, Long> {

    /**
     * Find all migrations in a given status
     * @param status Status search
     * @return
     */
    List<Migration> findAllByStatusOrderByDateDesc(StatusEnum status);
}
