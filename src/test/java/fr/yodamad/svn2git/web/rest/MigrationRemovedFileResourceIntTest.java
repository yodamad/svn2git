package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;

import fr.yodamad.svn2git.domain.MigrationRemovedFile;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.repository.MigrationRemovedFileRepository;
import fr.yodamad.svn2git.service.MigrationRemovedFileService;
import fr.yodamad.svn2git.web.rest.errors.ExceptionTranslator;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;


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

    private static final String DEFAULT_PATH = "AAAAAAAAAA";
    private static final String UPDATED_PATH = "BBBBBBBBBB";

    private static final Reason DEFAULT_REASON = Reason.EXTENSION;
    private static final Reason UPDATED_REASON = Reason.SIZE;

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
    private Validator validator;

    private MockMvc restMigrationRemovedFileMockMvc;

    private MigrationRemovedFile migrationRemovedFile;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MigrationRemovedFileResource migrationRemovedFileResource = new MigrationRemovedFileResource(migrationRemovedFileService);
        this.restMigrationRemovedFileMockMvc = MockMvcBuilders.standaloneSetup(migrationRemovedFileResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static MigrationRemovedFile createEntity(EntityManager em) {
        MigrationRemovedFile migrationRemovedFile = new MigrationRemovedFile()
            .path(DEFAULT_PATH)
            .reason(DEFAULT_REASON);
        // Add required entity
        Migration migration = MigrationResourceIntTest.createEntity(em);
        em.persist(migration);
        em.flush();
        migrationRemovedFile.setMigration(migration);
        return migrationRemovedFile;
    }

    @Before
    public void initTest() {
        migrationRemovedFile = createEntity(em);
    }

    @Test
    @Transactional
    public void createMigrationRemovedFile() throws Exception {
        int databaseSizeBeforeCreate = migrationRemovedFileRepository.findAll().size();

        // Create the MigrationRemovedFile
        restMigrationRemovedFileMockMvc.perform(post("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationRemovedFile)))
            .andExpect(status().isCreated());

        // Validate the MigrationRemovedFile in the database
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeCreate + 1);
        MigrationRemovedFile testMigrationRemovedFile = migrationRemovedFileList.get(migrationRemovedFileList.size() - 1);
        assertThat(testMigrationRemovedFile.getPath()).isEqualTo(DEFAULT_PATH);
        assertThat(testMigrationRemovedFile.getReason()).isEqualTo(DEFAULT_REASON);
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
    public void checkPathIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRemovedFileRepository.findAll().size();
        // set the field null
        migrationRemovedFile.setPath(null);

        // Create the MigrationRemovedFile, which fails.

        restMigrationRemovedFileMockMvc.perform(post("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationRemovedFile)))
            .andExpect(status().isBadRequest());

        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkReasonIsRequired() throws Exception {
        int databaseSizeBeforeTest = migrationRemovedFileRepository.findAll().size();
        // set the field null
        migrationRemovedFile.setReason(null);

        // Create the MigrationRemovedFile, which fails.

        restMigrationRemovedFileMockMvc.perform(post("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(migrationRemovedFile)))
            .andExpect(status().isBadRequest());

        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllMigrationRemovedFiles() throws Exception {
        // Initialize the database
        migrationRemovedFileRepository.saveAndFlush(migrationRemovedFile);

        // Get all the migrationRemovedFileList
        restMigrationRemovedFileMockMvc.perform(get("/api/migration-removed-files?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(migrationRemovedFile.getId().intValue())))
            .andExpect(jsonPath("$.[*].path").value(hasItem(DEFAULT_PATH.toString())))
            .andExpect(jsonPath("$.[*].reason").value(hasItem(DEFAULT_REASON.toString())));
    }
    
    @Test
    @Transactional
    public void getMigrationRemovedFile() throws Exception {
        // Initialize the database
        migrationRemovedFileRepository.saveAndFlush(migrationRemovedFile);

        // Get the migrationRemovedFile
        restMigrationRemovedFileMockMvc.perform(get("/api/migration-removed-files/{id}", migrationRemovedFile.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(migrationRemovedFile.getId().intValue()))
            .andExpect(jsonPath("$.path").value(DEFAULT_PATH.toString()))
            .andExpect(jsonPath("$.reason").value(DEFAULT_REASON.toString()));
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
        // Initialize the database
        migrationRemovedFileService.save(migrationRemovedFile);

        int databaseSizeBeforeUpdate = migrationRemovedFileRepository.findAll().size();

        // Update the migrationRemovedFile
        MigrationRemovedFile updatedMigrationRemovedFile = migrationRemovedFileRepository.findById(migrationRemovedFile.getId()).get();
        // Disconnect from session so that the updates on updatedMigrationRemovedFile are not directly saved in db
        em.detach(updatedMigrationRemovedFile);
        updatedMigrationRemovedFile
            .path(UPDATED_PATH)
            .reason(UPDATED_REASON);

        restMigrationRemovedFileMockMvc.perform(put("/api/migration-removed-files")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMigrationRemovedFile)))
            .andExpect(status().isOk());

        // Validate the MigrationRemovedFile in the database
        List<MigrationRemovedFile> migrationRemovedFileList = migrationRemovedFileRepository.findAll();
        assertThat(migrationRemovedFileList).hasSize(databaseSizeBeforeUpdate);
        MigrationRemovedFile testMigrationRemovedFile = migrationRemovedFileList.get(migrationRemovedFileList.size() - 1);
        assertThat(testMigrationRemovedFile.getPath()).isEqualTo(UPDATED_PATH);
        assertThat(testMigrationRemovedFile.getReason()).isEqualTo(UPDATED_REASON);
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
        // Initialize the database
        migrationRemovedFileService.save(migrationRemovedFile);

        int databaseSizeBeforeDelete = migrationRemovedFileRepository.findAll().size();

        // Delete the migrationRemovedFile
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
