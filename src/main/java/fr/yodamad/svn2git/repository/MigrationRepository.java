package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * @param user input user
     * @param pageable
     * @return all migrations invoked by user
     */
    Page<Migration> findAllByUser(String user, Pageable pageable);

    /**
     * @param group input group
     * @param pageable
     * @return all migrations concerning given group
     */
    Page<Migration> findAllBySvnGroup(String group, Pageable pageable);

    /**
     * @param project input project
     * @param pageable
     * @return all migrations concerning given project
     */
    Page<Migration> findAllBySvnProjectEndingWith(String project, Pageable pageable);
}
