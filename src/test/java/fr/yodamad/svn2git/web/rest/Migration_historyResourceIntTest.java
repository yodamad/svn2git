package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.service.MigrationHistoryService;
import fr.yodamad.svn2git.web.rest.errors.ExceptionTranslator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static fr.yodamad.svn2git.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the Migration_historyResource REST controller.
 *
 * @see MigrationHistoryResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class Migration_historyResourceIntTest {

    private static final StepEnum DEFAULT_STEP = StepEnum.SVN_CHECKOUT;
    private static final StepEnum UPDATED_STEP = StepEnum.GIT_PUSH;

    private static final StatusEnum DEFAULT_STATUS = StatusEnum.WAITING;
    private static final StatusEnum UPDATED_STATUS = StatusEnum.RUNNING;

    private static final Instant DEFAULT_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private MigrationHistoryRepository migration_historyRepository;

    @Autowired
    private MigrationHistoryService migration_historyService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restMigration_historyMockMvc;

    private MigrationHistory migration_history;

    @BeforeAll
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MigrationHistoryResource migration_historyResource = new MigrationHistoryResource(migration_historyService);
        this.restMigration_historyMockMvc = MockMvcBuilders.standaloneSetup(migration_historyResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MigrationHistory createEntity(EntityManager em) {
        MigrationHistory migration_history = new MigrationHistory()
            .step(DEFAULT_STEP)
            .status(DEFAULT_STATUS)
            .date(DEFAULT_DATE);
        return migration_history;
    }

    @BeforeEach
    public void initTest() {
        migration_history = createEntity(em);
    }

    @Test
    @Transactional
    public void createMigration_history() throws Exception {
        int databaseSizeBeforeCreate = migration_historyRepository.findAll().size();

        // Create the MigrationHistory
        restMigration_historyMockMvc.perform(post("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration_history)))
            .andExpect(status().isCreated());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migration_historyList = migration_historyRepository.findAll();
        assertThat(migration_historyList).hasSize(databaseSizeBeforeCreate + 1);
        MigrationHistory testMigration_history = migration_historyList.get(migration_historyList.size() - 1);
        Assertions.assertThat(testMigration_history.getStep()).isEqualTo(DEFAULT_STEP);
        Assertions.assertThat(testMigration_history.getStatus()).isEqualTo(DEFAULT_STATUS);
        assertThat(testMigration_history.getDate()).isEqualTo(DEFAULT_DATE);
    }

    @Test
    @Transactional
    public void createMigration_historyWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = migration_historyRepository.findAll().size();

        // Create the MigrationHistory with an existing ID
        migration_history.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMigration_historyMockMvc.perform(post("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration_history)))
            .andExpect(status().isBadRequest());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migration_historyList = migration_historyRepository.findAll();
        assertThat(migration_historyList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllMigration_histories() throws Exception {
        // Initialize the database
        migration_historyRepository.saveAndFlush(migration_history);

        // Get all the migration_historyList
        restMigration_historyMockMvc.perform(get("/api/migration-histories?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(migration_history.getId().intValue())))
            .andExpect(jsonPath("$.[*].step").value(hasItem(DEFAULT_STEP.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())));
    }

    @Test
    @Transactional
    public void getMigration_history() throws Exception {
        // Initialize the database
        migration_historyRepository.saveAndFlush(migration_history);

        // Get the migration_history
        restMigration_historyMockMvc.perform(get("/api/migration-histories/{id}", migration_history.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(migration_history.getId().intValue()))
            .andExpect(jsonPath("$.step").value(DEFAULT_STEP.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingMigration_history() throws Exception {
        // Get the migration_history
        restMigration_historyMockMvc.perform(get("/api/migration-histories/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMigration_history() throws Exception {
        // Initialize the database
        migration_historyService.save(migration_history);

        int databaseSizeBeforeUpdate = migration_historyRepository.findAll().size();

        // Update the migration_history
        MigrationHistory updatedMigration_history = migration_historyRepository.findById(migration_history.getId()).get();
        // Disconnect from session so that the updates on updatedMigration_history are not directly saved in db
        em.detach(updatedMigration_history);
        updatedMigration_history
            .step(UPDATED_STEP)
            .status(UPDATED_STATUS)
            .date(UPDATED_DATE);

        restMigration_historyMockMvc.perform(put("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMigration_history)))
            .andExpect(status().isOk());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migration_historyList = migration_historyRepository.findAll();
        assertThat(migration_historyList).hasSize(databaseSizeBeforeUpdate);
        MigrationHistory testMigration_history = migration_historyList.get(migration_historyList.size() - 1);
        Assertions.assertThat(testMigration_history.getStep()).isEqualTo(UPDATED_STEP);
        Assertions.assertThat(testMigration_history.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testMigration_history.getDate()).isEqualTo(UPDATED_DATE);
    }

    @Test
    @Transactional
    public void updateNonExistingMigration_history() throws Exception {
        int databaseSizeBeforeUpdate = migration_historyRepository.findAll().size();

        // Create the MigrationHistory

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMigration_historyMockMvc.perform(put("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration_history)))
            .andExpect(status().isBadRequest());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migration_historyList = migration_historyRepository.findAll();
        assertThat(migration_historyList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMigration_history() throws Exception {
        // Initialize the database
        migration_historyService.save(migration_history);

        int databaseSizeBeforeDelete = migration_historyRepository.findAll().size();

        // Get the migration_history
        restMigration_historyMockMvc.perform(delete("/api/migration-histories/{id}", migration_history.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<MigrationHistory> migration_historyList = migration_historyRepository.findAll();
        assertThat(migration_historyList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MigrationHistory.class);
        MigrationHistory migration_history1 = new MigrationHistory();
        migration_history1.setId(1L);
        MigrationHistory migration_history2 = new MigrationHistory();
        migration_history2.setId(migration_history1.getId());
        assertThat(migration_history1).isEqualTo(migration_history2);
        migration_history2.setId(2L);
        assertThat(migration_history1).isNotEqualTo(migration_history2);
        migration_history1.setId(null);
        assertThat(migration_history1).isNotEqualTo(migration_history2);
    }
}
