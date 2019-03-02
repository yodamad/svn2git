package fr.yodamad.svn2git.repository;

import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
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
     * @return all migrations invoked by user
     */
    List<Migration> findAllByUser(String user);

    /**
     * @param group input group
     * @return all migrations concerning given group
     */
    List<Migration> findAllBySvnGroup(String group);

    /**
     * @param project input project
     * @return all migrations concerning given project
     */
    List<Migration> findAllBySvnProjectEndingWith(String project);
}
