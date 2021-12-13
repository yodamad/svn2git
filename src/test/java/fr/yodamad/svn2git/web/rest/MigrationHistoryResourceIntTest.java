package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MigrationHistoryRepository;
import fr.yodamad.svn2git.service.MigrationHistoryService;
import fr.yodamad.svn2git.web.rest.errors.ExceptionTranslator;
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
 * Test class for the MigrationHistoryResource REST controller.
 *
 * @see MigrationHistoryResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class MigrationHistoryResourceIntTest {

    private static final StepEnum DEFAULT_STEP = StepEnum.GITLAB_PROJECT_CREATION;
    private static final StepEnum UPDATED_STEP = StepEnum.SVN_CHECKOUT;

    private static final StatusEnum DEFAULT_STATUS = StatusEnum.WAITING;
    private static final StatusEnum UPDATED_STATUS = StatusEnum.RUNNING;

    private static final Instant DEFAULT_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String DEFAULT_DATA = "AAAAAAAAAA";
    private static final String UPDATED_DATA = "BBBBBBBBBB";

    @Autowired
    private MigrationHistoryRepository migrationHistoryRepository;

    @Autowired
    private MigrationHistoryService migrationHistoryService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restMigrationHistoryMockMvc;

    private MigrationHistory migrationHistory;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MigrationHistoryResource migrationHistoryResource = new MigrationHistoryResource(migrationHistoryService);
        this.restMigrationHistoryMockMvc = MockMvcBuilders.standaloneSetup(migrationHistoryResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
        migrationHistory = createEntity(em);
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MigrationHistory createEntity(EntityManager em) {
        MigrationHistory migrationHistory = new MigrationHistory()
            .step(DEFAULT_STEP)
            .status(DEFAULT_STATUS)
            .date(DEFAULT_DATE)
            .data(DEFAULT_DATA);
        return migrationHistory;
    }

    @Test
    @Transactional
    public void createMigrationHistory() throws Exception {
        int databaseSizeBeforeCreate = migrationHistoryRepository.findAll().size();

        // Create the MigrationHistory
        restMigrationHistoryMockMvc.perform(post("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationHistory)))
            .andExpect(status().isCreated());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migrationHistoryList = migrationHistoryRepository.findAll();
        assertThat(migrationHistoryList).hasSize(databaseSizeBeforeCreate + 1);
        MigrationHistory testMigrationHistory = migrationHistoryList.get(migrationHistoryList.size() - 1);
        assertThat(testMigrationHistory.getStep()).isEqualTo(DEFAULT_STEP);
        assertThat(testMigrationHistory.getStatus()).isEqualTo(DEFAULT_STATUS);
        assertThat(testMigrationHistory.getDate()).isEqualTo(DEFAULT_DATE);
        assertThat(testMigrationHistory.getData()).isEqualTo(DEFAULT_DATA);
    }

    @Test
    @Transactional
    public void createMigrationHistoryWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = migrationHistoryRepository.findAll().size();

        // Create the MigrationHistory with an existing ID
        migrationHistory.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMigrationHistoryMockMvc.perform(post("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationHistory)))
            .andExpect(status().isBadRequest());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migrationHistoryList = migrationHistoryRepository.findAll();
        assertThat(migrationHistoryList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllMigrationHistories() throws Exception {
        // Initialize the database
        migrationHistoryRepository.saveAndFlush(migrationHistory);

        // Get all the migrationHistoryList
        restMigrationHistoryMockMvc.perform(get("/api/migration-histories?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(migrationHistory.getId().intValue())))
            .andExpect(jsonPath("$.[*].step").value(hasItem(DEFAULT_STEP.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA.toString())));
    }

    @Test
    @Transactional
    public void getMigrationHistory() throws Exception {
        // Initialize the database
        migrationHistoryRepository.saveAndFlush(migrationHistory);

        // Get the migrationHistory
        restMigrationHistoryMockMvc.perform(get("/api/migration-histories/{id}", migrationHistory.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(migrationHistory.getId().intValue()))
            .andExpect(jsonPath("$.step").value(DEFAULT_STEP.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.data").value(DEFAULT_DATA.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingMigrationHistory() throws Exception {
        // Get the migrationHistory
        restMigrationHistoryMockMvc.perform(get("/api/migration-histories/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMigrationHistory() throws Exception {
        // Initialize the database
        migrationHistoryService.save(migrationHistory);

        int databaseSizeBeforeUpdate = migrationHistoryRepository.findAll().size();

        // Update the migrationHistory
        MigrationHistory updatedMigrationHistory = migrationHistoryRepository.findById(migrationHistory.getId()).get();
        // Disconnect from session so that the updates on updatedMigrationHistory are not directly saved in db
        em.detach(updatedMigrationHistory);
        updatedMigrationHistory
            .step(UPDATED_STEP)
            .status(UPDATED_STATUS)
            .date(UPDATED_DATE)
            .data(UPDATED_DATA);

        restMigrationHistoryMockMvc.perform(put("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMigrationHistory)))
            .andExpect(status().isOk());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migrationHistoryList = migrationHistoryRepository.findAll();
        assertThat(migrationHistoryList).hasSize(databaseSizeBeforeUpdate);
        MigrationHistory testMigrationHistory = migrationHistoryList.get(migrationHistoryList.size() - 1);
        assertThat(testMigrationHistory.getStep()).isEqualTo(UPDATED_STEP);
        assertThat(testMigrationHistory.getStatus()).isEqualTo(UPDATED_STATUS);
        assertThat(testMigrationHistory.getDate()).isEqualTo(UPDATED_DATE);
        assertThat(testMigrationHistory.getData()).isEqualTo(UPDATED_DATA);
    }

    @Test
    @Transactional
    public void updateNonExistingMigrationHistory() throws Exception {
        int databaseSizeBeforeUpdate = migrationHistoryRepository.findAll().size();

        // Create the MigrationHistory

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMigrationHistoryMockMvc.perform(put("/api/migration-histories")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationHistory)))
            .andExpect(status().isBadRequest());

        // Validate the MigrationHistory in the database
        List<MigrationHistory> migrationHistoryList = migrationHistoryRepository.findAll();
        assertThat(migrationHistoryList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMigrationHistory() throws Exception {
        // Initialize the database
        migrationHistoryService.save(migrationHistory);

        int databaseSizeBeforeDelete = migrationHistoryRepository.findAll().size();

        // Get the migrationHistory
        restMigrationHistoryMockMvc.perform(delete("/api/migration-histories/{id}", migrationHistory.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<MigrationHistory> migrationHistoryList = migrationHistoryRepository.findAll();
        assertThat(migrationHistoryList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MigrationHistory.class);
        MigrationHistory migrationHistory1 = new MigrationHistory();
        migrationHistory1.setId(1L);
        MigrationHistory migrationHistory2 = new MigrationHistory();
        migrationHistory2.setId(migrationHistory1.getId());
        assertThat(migrationHistory1).isEqualTo(migrationHistory2);
        migrationHistory2.setId(2L);
        assertThat(migrationHistory1).isNotEqualTo(migrationHistory2);
        migrationHistory1.setId(null);
        assertThat(migrationHistory1).isNotEqualTo(migrationHistory2);
    }
}
