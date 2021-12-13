package fr.yodamad.svn2git.web.rest;

import com.jayway.jsonpath.JsonPath;
import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MappingService;
import fr.yodamad.svn2git.service.MigrationHistoryService;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.web.rest.errors.ExceptionTranslator;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Assertions;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static fr.yodamad.svn2git.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the MigrationResource REST controller.
 *
 * @see MigrationResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class MigrationResourceIntTest {

    public static final String DEFAULT_SVN_GROUP = "AAAAAAAAAA";
    private static final String UPDATED_SVN_GROUP = "BBBBBBBBBB";

    public static final String DEFAULT_SVN_PROJECT = "AAAAAAAAAA";
    private static final String UPDATED_SVN_PROJECT = "BBBBBBBBBB";

    public static final String DEFAULT_USER = "AAAAAAAAAA";
    private static final String UPDATED_USER = "BBBBBBBBBB";

    private static final LocalDate DEFAULT_DATE = LocalDate.ofEpochDay(0L);
    public static final LocalDate UPDATED_DATE = LocalDate.now(ZoneId.systemDefault());

    public static final String DEFAULT_GITLAB_GROUP = "AAAAAAAAAA";
    private static final String UPDATED_GITLAB_GROUP = "BBBBBBBBBB";

    public static final String DEFAULT_GITLAB_PROJECT = "AAAAAAAAAA";
    private static final String UPDATED_GITLAB_PROJECT = "BBBBBBBBBB";

    public static final StatusEnum DEFAULT_STATUS = StatusEnum.RUNNING;
    public static final StatusEnum WAITING_STATUS = StatusEnum.WAITING;
    public static final StatusEnum UPDATED_STATUS = StatusEnum.DONE;

    @Autowired
    private MigrationRepository migrationRepository;

    @Autowired
    private MigrationManager migrationManager;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restMigrationMockMvc;

    private Migration migration;

    @Autowired
    private MigrationHistoryService migrationHistoryService;

    @Autowired
    private MappingService mappingService;

    @Autowired
    private GitlabResource gitlabResource;

    @Autowired
    private ApplicationProperties applicationProperties;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MigrationResource migrationResource = new MigrationResource(migrationRepository, migrationManager, migrationHistoryService, mappingService, gitlabResource, applicationProperties);
        this.restMigrationMockMvc = MockMvcBuilders.standaloneSetup(migrationResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
        migration = createEntity(em);
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Migration createEntity(EntityManager em) {
        Migration migration = new Migration()
            .svnGroup(DEFAULT_SVN_GROUP)
            .svnProject(DEFAULT_SVN_PROJECT)
            .user(DEFAULT_USER)
            .date(DEFAULT_DATE)
            .gitlabGroup(DEFAULT_GITLAB_GROUP)
            .gitlabProject(DEFAULT_GITLAB_PROJECT)
            .status(DEFAULT_STATUS);

        return migration;
    }

    @Test
    @Transactional
    public void createMigration() throws Exception {
        int databaseSizeBeforeCreate = migrationRepository.findAll().size();

        // Create the Migration
        MvcResult mvcResult = restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isCreated()).andReturn();

        // System.out.println(mvcResult.getResponse().getContentAsString());
        Integer id = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id");

        // Validate the Migration in the database
        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeCreate + 1);
        Migration testMigration = migrationList.get(migrationList.size() - 1);
        assertThat(testMigration.getSvnGroup()).isEqualTo(DEFAULT_SVN_GROUP);
        assertThat(testMigration.getSvnProject()).isEqualTo(DEFAULT_SVN_PROJECT);
        assertThat(testMigration.getUser()).isEqualTo(DEFAULT_USER);
        assertThat(testMigration.getDate()).isEqualTo(UPDATED_DATE);
        assertThat(testMigration.getGitlabGroup()).isEqualTo(DEFAULT_GITLAB_GROUP);
        assertThat(testMigration.getGitlabProject()).isEqualTo(DEFAULT_GITLAB_PROJECT);
        Assertions.assertThat(testMigration.getStatus()).isEqualTo(WAITING_STATUS);
    }

    @Test
    @Transactional
    public void createMigrationWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = migrationRepository.findAll().size();

        // Create the Migration with an existing ID
        migration.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        // Validate the Migration in the database
        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkSvn_groupIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRepository.findAll().size();
        // set the field null
        migration.setSvnGroup(null);
        migration.setId(RandomUtils.nextLong());

        // Create the Migration, which fails.
        restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkSvn_projectIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRepository.findAll().size();
        // set the field null
        migration.setSvnProject(null);
        migration.setId(RandomUtils.nextLong());

        // Create the Migration, which fails.
        restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkUserIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRepository.findAll().size();
        // set the field null
        migration.setUser(null);
        migration.setId(RandomUtils.nextLong());

        // Create the Migration, which fails.
        restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGitlab_groupIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRepository.findAll().size();
        // set the field null
        migration.setGitlabGroup(null);
        migration.setId(RandomUtils.nextLong());

        // Create the Migration, which fails.
        restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkGitlab_projectIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRepository.findAll().size();
        // set the field null
        migration.setGitlabProject(null);
        migration.setId(RandomUtils.nextLong());

        // Create the Migration, which fails.
        restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMigrations() throws Exception {
        // Initialize the database
        migrationRepository.saveAndFlush(migration);

        // Get all the migrationList
        restMigrationMockMvc.perform(get("/api/migrations?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(migration.getId().intValue())))
            .andExpect(jsonPath("$.[*].svnGroup").value(hasItem(DEFAULT_SVN_GROUP.toString())))
            .andExpect(jsonPath("$.[*].svnProject").value(hasItem(DEFAULT_SVN_PROJECT.toString())))
            .andExpect(jsonPath("$.[*].user").value(hasItem(DEFAULT_USER.toString())))
            .andExpect(jsonPath("$.[*].date").value(hasItem(DEFAULT_DATE.toString())))
            .andExpect(jsonPath("$.[*].gitlabGroup").value(hasItem(DEFAULT_GITLAB_GROUP.toString())))
            .andExpect(jsonPath("$.[*].gitlabProject").value(hasItem(DEFAULT_GITLAB_PROJECT.toString())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @Test
    @Transactional
    public void getMigration() throws Exception {
        // Initialize the database
        migrationRepository.saveAndFlush(migration);

        // Get the migration
        MvcResult restMigrationMockMvcResult = restMigrationMockMvc.perform(get("/api/migrations/{id}", migration.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(migration.getId().intValue()))
            .andExpect(jsonPath("$.svnGroup").value(DEFAULT_SVN_GROUP.toString()))
            .andExpect(jsonPath("$.svnProject").value(DEFAULT_SVN_PROJECT.toString()))
            .andExpect(jsonPath("$.user").value(DEFAULT_USER.toString()))
            .andExpect(jsonPath("$.date").value(DEFAULT_DATE.toString()))
            .andExpect(jsonPath("$.gitlabGroup").value(DEFAULT_GITLAB_GROUP.toString()))
            .andExpect(jsonPath("$.gitlabProject").value(DEFAULT_GITLAB_PROJECT.toString()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()))
            .andReturn();

        // For debugging
        // System.out.println(restMigrationMockMvcResult.getResponse().getContentAsString());
        // Integer id = JsonPath.read(restMigrationMockMvcResult.getResponse().getContentAsString(), "$.id");

    }

    @Test
    @Transactional
    public void getNonExistingMigration() throws Exception {
        // Get the migration
        restMigrationMockMvc.perform(get("/api/migrations/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMigration() throws Exception {
        // Initialize the database
        migrationRepository.saveAndFlush(migration);

        int databaseSizeBeforeUpdate = migrationRepository.findAll().size();

        // Update the migration
        Migration updatedMigration = migrationRepository.findById(migration.getId()).get();
        // Disconnect from session so that the updates on updatedMigration are not directly saved in db
        em.detach(updatedMigration);
        updatedMigration
            .svnGroup(UPDATED_SVN_GROUP)
            .svnProject(UPDATED_SVN_PROJECT)
            .user(UPDATED_USER)
            .date(UPDATED_DATE)
            .gitlabGroup(UPDATED_GITLAB_GROUP)
            .gitlabProject(UPDATED_GITLAB_PROJECT)
            .status(UPDATED_STATUS);

        restMigrationMockMvc.perform(put("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMigration)))
            .andExpect(status().isOk());

        // Validate the Migration in the database
        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeUpdate);
        Migration testMigration = migrationList.get(migrationList.size() - 1);
        assertThat(testMigration.getSvnGroup()).isEqualTo(UPDATED_SVN_GROUP);
        assertThat(testMigration.getSvnProject()).isEqualTo(UPDATED_SVN_PROJECT);
        assertThat(testMigration.getUser()).isEqualTo(UPDATED_USER);
        assertThat(testMigration.getDate()).isEqualTo(UPDATED_DATE);
        assertThat(testMigration.getGitlabGroup()).isEqualTo(UPDATED_GITLAB_GROUP);
        assertThat(testMigration.getGitlabProject()).isEqualTo(UPDATED_GITLAB_PROJECT);
        Assertions.assertThat(testMigration.getStatus()).isEqualTo(UPDATED_STATUS);
    }

    @Test
    @Transactional
    public void updateNonExistingMigration() throws Exception {
        int databaseSizeBeforeUpdate = migrationRepository.findAll().size();

        // Create the Migration

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMigrationMockMvc.perform(put("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isBadRequest());

        // Validate the Migration in the database
        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMigration() throws Exception {
        // Initialize the database
        migrationRepository.saveAndFlush(migration);

        int databaseSizeBeforeDelete = migrationRepository.findAll().size();

        // Get the migration
        restMigrationMockMvc.perform(delete("/api/migrations/{id}", migration.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Migration.class);
        Migration migration1 = new Migration();
        migration1.setId(1L);
        Migration migration2 = new Migration();
        migration2.setId(migration1.getId());
        assertThat(migration1).isEqualTo(migration2);
        migration2.setId(2L);
        assertThat(migration1).isNotEqualTo(migration2);
        migration1.setId(null);
        assertThat(migration1).isNotEqualTo(migration2);
    }
}
