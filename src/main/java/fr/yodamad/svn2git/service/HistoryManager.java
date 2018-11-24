package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Migration history operations
 */
@Service
public class HistoryManager {

    private final MigrationHistoryRepository migrationHistoryRepository;

    public HistoryManager(final MigrationHistoryRepository mhr) {
        this.migrationHistoryRepository = mhr;
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
            .status(StatusEnum.RUNNING);

        if (data != null) {
            history.data(data);
        }

        return migrationHistoryRepository.save(history);
    }

    /**
     * Update history
     * @param history
     */
    public void endStep(MigrationHistory history, StatusEnum status, String data) {
        history.setStatus(status);
        if (data != null) history.setData(data);
        migrationHistoryRepository.save(history);
    }
}
