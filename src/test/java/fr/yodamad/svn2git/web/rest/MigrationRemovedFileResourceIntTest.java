package fr.yodamad.svn2git.web.rest;

import com.jayway.jsonpath.JsonPath;
import fr.yodamad.svn2git.Svn2GitApp;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.repository.MigrationRemovedFileRepository;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MappingService;
import fr.yodamad.svn2git.service.MigrationHistoryService;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.service.MigrationRemovedFileService;
import fr.yodamad.svn2git.web.rest.errors.ExceptionTranslator;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;


import static fr.yodamad.svn2git.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import fr.yodamad.svn2git.domain.enumeration.Reason;
/**
 * Test class for the MigrationRemovedFileResource REST controller.
 *
 * @see MigrationRemovedFileResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class MigrationRemovedFileResourceIntTest {

    private static final String DEFAULT_SVN_LOCATION = "AAAAAAAAAA";
    private static final String UPDATED_SVN_LOCATION = "BBBBBBBBBB";

    private static final String DEFAULT_PATH = "AAAAAAAAAA";
    private static final String UPDATED_PATH = "BBBBBBBBBB";

    private static final Reason DEFAULT_REASON = Reason.EXTENSION;
    private static final Reason UPDATED_REASON = Reason.SIZE;

    private static final Long DEFAULT_FILE_SIZE = 1L;
    private static final Long UPDATED_FILE_SIZE = 2L;

    @Autowired
    private MigrationRemovedFileRepository migrationRemovedFileRepository;

    @Autowired
    private MigrationRemovedFileService migrationRemovedFileService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private MigrationRepository migrationRepository;

    private MockMvc restMigrationRemovedFileMockMvc;

    private MigrationRemovedFile migrationRemovedFile;

    /**
     * for creation of supporting Migration record
     */
    private MockMvc restMigrationMockMvc;

    /**
     * for creation of supporting Migration record
     */
    private Migration migration;

    /**
     * for creation of supporting Migration record
     */
    @Autowired
    private MigrationManager migrationManager;

    /**
     * for creation of supporting Migration record
     */
    @Autowired
    private MigrationHistoryService migrationHistoryService;

    /**
     * for creation of supporting Migration record
     */
    @Autowired
    private MappingService mappingService;

    /**
     * for creation of supporting Migration record
     */
    @Autowired
    private GitlabResource gitlabResource;

    /**
     * for creation of supporting Migration record
     */
    @Autowired
    private ApplicationProperties applicationProperties;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MigrationRemovedFileResource migrationRemovedFileResource = new MigrationRemovedFileResource(migrationRemovedFileService);
        this.restMigrationRemovedFileMockMvc = MockMvcBuilders.standaloneSetup(migrationRemovedFileResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();

        final MigrationResource migrationResource = new MigrationResource(migrationRepository, migrationManager, migrationHistoryService, mappingService, gitlabResource, applicationProperties);
        this.restMigrationMockMvc = MockMvcBuilders.standaloneSetup(migrationResource)
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
    public static MigrationRemovedFile createEntity(EntityManager em) {
        MigrationRemovedFile migrationRemovedFile = new MigrationRemovedFile()
            .svnLocation(DEFAULT_SVN_LOCATION)
            .path(DEFAULT_PATH)
            .reason(DEFAULT_REASON)
            .fileSize(DEFAULT_FILE_SIZE);
        return migrationRemovedFile;
    }

    @Before
    public void initTest() {
        migrationRemovedFile = createEntity(em);
        // create an instance of Migration
        migration = MigrationResourceIntTest.createEntity(em);
    }

    @Test
    @Transactional
    public void createMigrationRemovedFile() throws Exception {
        int databaseSizeBeforeCreate = migrationRemovedFileRepository.findAll().size();

        Migration migration = createMigration();
        migrationRemovedFile.setMigration(migration);

        // Create the MigrationRemovedFile
        restMigrationRemovedFileMockMvc.perform(post("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationRemovedFile)))
            .andExpect(status().isCreated());

        // Validate the MigrationRemovedFile in the database
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeCreate + 1);
        MigrationRemovedFile testMigrationRemovedFile = migrationRemovedFileList.get(migrationRemovedFileList.size() - 1);
        assertThat(testMigrationRemovedFile.getSvnLocation()).isEqualTo(DEFAULT_SVN_LOCATION);
        assertThat(testMigrationRemovedFile.getPath()).isEqualTo(DEFAULT_PATH);
        assertThat(testMigrationRemovedFile.getReason()).isEqualTo(DEFAULT_REASON);
        assertThat(testMigrationRemovedFile.getFileSize()).isEqualTo(DEFAULT_FILE_SIZE);
    }

    @Test
    @Transactional
    public void createMigrationRemovedFileWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = migrationRemovedFileRepository.findAll().size();

        // Create the MigrationRemovedFile with an existing ID
        migrationRemovedFile.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMigrationRemovedFileMockMvc.perform(post("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationRemovedFile)))
            .andExpect(status().isBadRequest());

        // Validate the MigrationRemovedFile in the database
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllMigrationRemovedFiles() throws Exception {

        Migration migration = createMigration();
        migrationRemovedFile.setMigration(migration);

        // Initialize the database
        migrationRemovedFileRepository.saveAndFlush(migrationRemovedFile);

        // Get all the migrationRemovedFileList
        restMigrationRemovedFileMockMvc.perform(get("/api/migration-removed-files?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(migrationRemovedFile.getId().intValue())))
            .andExpect(jsonPath("$.[*].svnLocation").value(hasItem(DEFAULT_SVN_LOCATION.toString())))
            .andExpect(jsonPath("$.[*].path").value(hasItem(DEFAULT_PATH.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON.toString())))
            .andExpect(jsonPath("$.[*].fileSize").value(hasItem(DEFAULT_FILE_SIZE.intValue())));
    }


    @Test
    @Transactional
    public void getMigrationRemovedFile() throws Exception {

        Migration migration = createMigration();
        migrationRemovedFile.setMigration(migration);

        // Initialize the database
        migrationRemovedFileRepository.saveAndFlush(migrationRemovedFile);

        // Get the migrationRemovedFile
        restMigrationRemovedFileMockMvc.perform(get("/api/migration-removed-files/{id}", migrationRemovedFile.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(migrationRemovedFile.getId().intValue()))
            .andExpect(jsonPath("$.svnLocation").value(DEFAULT_SVN_LOCATION.toString()))
            .andExpect(jsonPath("$.path").value(DEFAULT_PATH.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON.toString()))
            .andExpect(jsonPath("$.fileSize").value(DEFAULT_FILE_SIZE.intValue()));
    }

    /**
     * Supporting method for creation of a Migration record that can be attached to a MigrationRemovedFile
     * @throws Exception
     */
    @Transactional
    public Migration createMigration() throws Exception {

        int databaseSizeBeforeCreate = migrationRepository.findAll().size();

        // Create the Migration
        MvcResult mvcResult = restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migration)))
            .andExpect(status().isCreated())
            .andReturn();

        System.out.println(mvcResult.getResponse().getContentAsString());
        Integer id = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id");

        // Validate the Migration in the database
        List<Migration> migrationList = migrationRepository.findAll();
        assertThat(migrationList).hasSize(databaseSizeBeforeCreate + 1);
        Migration testMigration = migrationList.get(migrationList.size() - 1);
        assertThat(testMigration.getSvnGroup()).isEqualTo(MigrationResourceIntTest.DEFAULT_SVN_GROUP);
        assertThat(testMigration.getSvnProject()).isEqualTo(MigrationResourceIntTest.DEFAULT_SVN_PROJECT);
        assertThat(testMigration.getUser()).isEqualTo(MigrationResourceIntTest.DEFAULT_USER);
        assertThat(testMigration.getDate()).isEqualTo(MigrationResourceIntTest.UPDATED_DATE);
        assertThat(testMigration.getGitlabGroup()).isEqualTo(MigrationResourceIntTest.DEFAULT_GITLAB_GROUP);
        assertThat(testMigration.getGitlabProject()).isEqualTo(MigrationResourceIntTest.DEFAULT_GITLAB_PROJECT);
        Assertions.assertThat(testMigration.getStatus()).isEqualTo(MigrationResourceIntTest.WAITING_STATUS);

        // Get the migration object and return it.
        Optional<Migration> migration = migrationRepository.findById(id.longValue());
        assertThat(migration.isPresent()).isEqualTo(true);

        return migration.get();

    }



    @Test
    @Transactional
    public void getNonExistingMigrationRemovedFile() throws Exception {
        // Get the migrationRemovedFile
        restMigrationRemovedFileMockMvc.perform(get("/api/migration-removed-files/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMigrationRemovedFile() throws Exception {

        Migration migration = createMigration();
        migrationRemovedFile.setMigration(migration);

        // Initialize the database
        migrationRemovedFileService.save(migrationRemovedFile);

        int databaseSizeBeforeUpdate = migrationRemovedFileRepository.findAll().size();

        // Update the migrationRemovedFile
        MigrationRemovedFile updatedMigrationRemovedFile = migrationRemovedFileRepository.findById(migrationRemovedFile.getId()).get();
        // Disconnect from session so that the updates on updatedMigrationRemovedFile are not directly saved in db
        em.detach(updatedMigrationRemovedFile);
        updatedMigrationRemovedFile
            .svnLocation(UPDATED_SVN_LOCATION)
            .path(UPDATED_PATH)
            .reason(UPDATED_REASON)
            .fileSize(UPDATED_FILE_SIZE);

        restMigrationRemovedFileMockMvc.perform(put("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMigrationRemovedFile)))
            .andExpect(status().isOk());

        // Validate the MigrationRemovedFile in the database
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeUpdate);
        MigrationRemovedFile testMigrationRemovedFile = migrationRemovedFileList.get(migrationRemovedFileList.size() - 1);
        assertThat(testMigrationRemovedFile.getSvnLocation()).isEqualTo(UPDATED_SVN_LOCATION);
        assertThat(testMigrationRemovedFile.getPath()).isEqualTo(UPDATED_PATH);
        assertThat(testMigrationRemovedFile.getReason()).isEqualTo(UPDATED_REASON);
        assertThat(testMigrationRemovedFile.getFileSize()).isEqualTo(UPDATED_FILE_SIZE);
    }

    @Test
    @Transactional
    public void updateNonExistingMigrationRemovedFile() throws Exception {
        int databaseSizeBeforeUpdate = migrationRemovedFileRepository.findAll().size();

        // Create the MigrationRemovedFile

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMigrationRemovedFileMockMvc.perform(put("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationRemovedFile)))
            .andExpect(status().isBadRequest());

        // Validate the MigrationRemovedFile in the database
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMigrationRemovedFile() throws Exception {

        Migration migration = createMigration();
        migrationRemovedFile.setMigration(migration);

        // Initialize the database
        migrationRemovedFileService.save(migrationRemovedFile);

        int databaseSizeBeforeDelete = migrationRemovedFileRepository.findAll().size();

        // Get the migrationRemovedFile
        restMigrationRemovedFileMockMvc.perform(delete("/api/migration-removed-files/{id}", migrationRemovedFile.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(MigrationRemovedFile.class);
        MigrationRemovedFile migrationRemovedFile1 = new MigrationRemovedFile();
        migrationRemovedFile1.setId(1L);
        MigrationRemovedFile migrationRemovedFile2 = new MigrationRemovedFile();
        migrationRemovedFile2.setId(migrationRemovedFile1.getId());
        assertThat(migrationRemovedFile1).isEqualTo(migrationRemovedFile2);
        migrationRemovedFile2.setId(2L);
        assertThat(migrationRemovedFile1).isNotEqualTo(migrationRemovedFile2);
        migrationRemovedFile1.setId(null);
        assertThat(migrationRemovedFile1).isNotEqualTo(migrationRemovedFile2);
    }
}
