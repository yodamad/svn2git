package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.MigrationHistory
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.domain.enumeration.StepEnum
import fr.yodamad.svn2git.repository.MigrationHistoryRepository
import fr.yodamad.svn2git.repository.MigrationRepository
import fr.yodamad.svn2git.service.util.DateFormatter
import org.hibernate.Hibernate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Migration history operations
 */
@Service
open class HistoryManager(private val migrationHistoryRepository: MigrationHistoryRepository,
                          private val migrationRepository: MigrationRepository) {
    /**
     * Create a new history for migration
     * @param migration
     * @param step
     * @param data
     * @return
     */
    open fun startStep(migration: Migration?, step: StepEnum?, data: String?): MigrationHistory {
        val history = MigrationHistory()
            .step(step)
            .migration(migration)
            .date(Instant.now())
            .status(StatusEnum.RUNNING)
            .startTime(Instant.now())
        if (data != null) {
            history.data(data)
        }
        LOG.info("Start step $step")
        return migrationHistoryRepository.save(history)
    }

    /**
     * Update history
     * @param history
     */
    open fun endStep(history: MigrationHistory?, status: StatusEnum?, data: String? = null) {
        history?.status = status
        if (data != null) history?.data = data
        if (history?.startTime == null) {
            history?.executionTime = "N/A"
        } else {
            // Compute executionTime
            val execution = Instant.now().toEpochMilli() - history.startTime.toEpochMilli()
            history.executionTime = DateFormatter.toNiceFormat(execution)
        }
        migrationHistoryRepository.save(history!!)
        LOG.info("Finish step ${history.step} with status $status in ${history.executionTime}")
    }

    /**
     * Load eagerly a migration
     * @param migId Migration ID
     * @return migration loaded
     */
    @Transactional
    open fun loadMigration(migId: Long): Migration {
        val migration = migrationRepository.getById(migId)
        Hibernate.initialize(migration)
        Hibernate.initialize(migration.histories)
        Hibernate.initialize(migration.mappings)
        return migration
    }

    /**
     * Force flush for summary
     */
    open fun forceFlush() {
        migrationHistoryRepository.flush()
        migrationRepository.flush()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(HistoryManager::class.java)
    }
}
