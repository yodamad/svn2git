package fr.yodamad.svn2git.service

import fr.yodamad.svn2git.domain.Migration
import fr.yodamad.svn2git.domain.enumeration.StatusEnum
import fr.yodamad.svn2git.repository.MigrationRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Boolean
import java.util.function.Consumer
import javax.annotation.PostConstruct

/**
 * Component to check migrations
 */
@Service
@Lazy(false)
open class MigrationChecker(
    /** Migration manager.  */
    private val manager: MigrationManager,
    /** Migration repo.  */
    private val repository: MigrationRepository) {

    /**
     * Check if application crashed, if so restarts waiting migrations and fails those which are "running"
     */
    @Transactional
    @PostConstruct
    @DependsOn("asyncConfiguration")
    open fun checkDb() {
        try {
            // Start waiting migrations
            repository.findAllByStatusOrderByDateDesc(StatusEnum.WAITING).forEach(
                Consumer { mig: Migration -> manager.startMigration(mig.id, Boolean.FALSE) })
            // Fail running migrations
            repository.findAllByStatusOrderByDateDesc(StatusEnum.RUNNING).forEach(
                Consumer { mig: Migration ->
                    mig.status(StatusEnum.FAILED)
                    repository.save(mig)
                })
        } catch (exc: Exception) {
            LOG.error("Failed to check migration on startup", exc)
            // Fail running migrations
            repository.findAllByStatusOrderByDateDesc(StatusEnum.RUNNING).forEach(
                Consumer { mig: Migration ->
                    mig.status(StatusEnum.FAILED)
                    repository.save(mig)
                })
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(MigrationChecker::class.java)
    }
}
