package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.util.DateFormatter;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static java.lang.String.format;

/**
 * Migration history operations
 */
@Service
public class HistoryManager {

    private static final Logger LOG = LoggerFactory.getLogger(HistoryManager.class);
    private final MigrationHistoryRepository migrationHistoryRepository;
    private final MigrationRepository migrationRepository;

    public HistoryManager(final MigrationHistoryRepository mhr, final MigrationRepository mr) {
        this.migrationHistoryRepository = mhr;
        this.migrationRepository = mr;
    }

    /**
     * Create a new history for migration
     * @param migration
     * @param step
     * @param data
     * @return
     */
    public MigrationHistory startStep(Migration migration, StepEnum step, String data) {
        MigrationHistory history = new MigrationHistory()
            .step(step)
            .migration(migration)
            .date(Instant.now())
            .status(StatusEnum.RUNNING)
            .startTime(Instant.now());

        if (data != null) {
            history.data(data);
        }

        LOG.info(format("Start step %s", step));
        return migrationHistoryRepository.save(history);
    }

    /**
     * Update history
     * @param history
     */
    public void endStep(MigrationHistory history, StatusEnum status, String data) {
        history.setStatus(status);
        if (data != null) history.setData(data);

        if (history.getStartTime() == null) { history.setExecutionTime("N/A"); }
        else {
            // Compute executionTime
            Long execution = Instant.now().toEpochMilli() - history.getStartTime().toEpochMilli();
            history.setExecutionTime(DateFormatter.toNiceFormat(execution));
        }

        migrationHistoryRepository.save(history);
        LOG.info(format("Finish step %s with status %s in %s", history.getStep(), status, history.getExecutionTime()));
    }

    /**
     * Load eagerly a migration
     * @param migId Migration ID
     * @return migration loaded
     */
    @Transactional
    public Migration loadMigration(Long migId) {
        Migration migration = migrationRepository.getOne(migId);
        Hibernate.initialize(migration);
        Hibernate.initialize(migration.getHistories());
        Hibernate.initialize(migration.getMappings());
        return migration;
    }

    /**
     * Force flush for summary
     */
    public void forceFlush() {
        migrationHistoryRepository.flush();
        migrationRepository.flush();
    }
}
