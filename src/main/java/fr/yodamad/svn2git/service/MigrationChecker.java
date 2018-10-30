package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;

/**
 * Component to check migrations
 */
@Service
@Lazy(false)
public class MigrationChecker {

    /** Migration manager. */
    private final MigrationManager manager;
    /** Migration repo. */
    private final MigrationRepository repository;

    public MigrationChecker(MigrationManager migrationManager,
                              MigrationRepository migrationRepository) {
        this.manager = migrationManager;
        this.repository = migrationRepository;
    }

    /**
     * Check if application crashed, if so restarts waiting migrations and fails those which are "running"
     */
    @Transactional
    @PostConstruct
    @DependsOn("asyncConfiguration")
    public void checkDb() {
        // Start waiting migrations
        repository.findAllByStatusOrderByDateDesc(StatusEnum.WAITING).stream().forEach(mig -> manager.startMigration(mig.getId()));
        // Fail running migrations
        repository.findAllByStatusOrderByDateDesc(StatusEnum.RUNNING).stream().forEach(mig -> {
            mig.status(StatusEnum.FAILED);
            repository.save(mig);
        });
    }
}
