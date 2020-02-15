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
     * Find Migrations that are running or waiting (i.e. can query using a list of StatusEnum as parameter)
     * @param statusList List of StatusEnum for in clause
     * @return
     */
    List<Migration> findAllByStatusInOrderByDateDesc(List<StatusEnum> statusList);

    /**
     * @param user input user
     * @return all migrations invoked by user
     */
    List<Migration> findAllByUserIgnoreCaseOrderByIdDesc(String user);

    /**
     * @param group input group
     * @return all migrations concerning given group
     */
    List<Migration> findAllBySvnGroupIgnoreCaseOrderByIdDesc(String group);

    /**
     * @param project input project
     * @return all migrations concerning given project
     */
    List<Migration> findAllBySvnProjectEndingWithIgnoreCaseOrderByIdDesc(String project);
}
