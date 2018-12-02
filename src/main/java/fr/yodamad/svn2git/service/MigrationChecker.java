package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOG = LoggerFactory.getLogger(MigrationChecker.class);

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
        try {
            // Start waiting migrations
            repository.findAllByStatusOrderByDateDesc(StatusEnum.WAITING).stream().forEach(mig -> manager.startMigration(mig.getId()));
            // Fail running migrations
            repository.findAllByStatusOrderByDateDesc(StatusEnum.RUNNING).stream().forEach(mig -> {
                mig.status(StatusEnum.FAILED);
                repository.save(mig);
            });
        } catch (Exception exc) {
            LOG.error("Failed to check migration on startup", exc);
            // Fail running migrations
            repository.findAllByStatusOrderByDateDesc(StatusEnum.RUNNING).stream().forEach(mig -> {
                mig.status(StatusEnum.FAILED);
                repository.save(mig);
            });
        }
    }
}
