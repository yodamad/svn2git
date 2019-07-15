package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.Mapping;
import fr.yodamad.svn2git.repository.MappingRepository;
import fr.yodamad.svn2git.service.MappingService;
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

import javax.persistence.EntityManager;
import java.util.List;

import static fr.yodamad.svn2git.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the MappingResource REST controller.
 *
 * @see MappingResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class MappingResourceIntTest {

    private static final String DEFAULT_SVN_DIRECTORY = "AAAAAAAAAA";
    private static final String UPDATED_SVN_DIRECTORY = "BBBBBBBBBB";

    private static final String DEFAULT_REGEX = "AAAAAAAAAA";
    private static final String UPDATED_REGEX = "BBBBBBBBBB";

    private static final String DEFAULT_GIT_DIRECTORY = "AAAAAAAAAA";
    private static final String UPDATED_GIT_DIRECTORY = "BBBBBBBBBB";

    private static final Boolean DEFAULT_SVN_DIRECTORY_DELETE = false;
    private static final Boolean UPDATED_SVN_DIRECTORY_DELETE = true;

    @Autowired
    private MappingRepository mappingRepository;
    
    @Autowired
    private MappingService mappingService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restMappingMockMvc;

    private Mapping mapping;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final MappingResource mappingResource = new MappingResource(mappingService);
        this.restMappingMockMvc = MockMvcBuilders.standaloneSetup(mappingResource)
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
    public static Mapping createEntity(EntityManager em) {
        Mapping mapping = new Mapping()
            .svnDirectory(DEFAULT_SVN_DIRECTORY)
            .regex(DEFAULT_REGEX)
            .gitDirectory(DEFAULT_GIT_DIRECTORY)
            .svnDirectoryDelete(DEFAULT_SVN_DIRECTORY_DELETE);
        return mapping;
    }

    @Before
    public void initTest() {
        mapping = createEntity(em);
    }

    @Test
    @Transactional
    public void createMapping() throws Exception {
        int databaseSizeBeforeCreate = mappingRepository.findAll().size();

        // Create the Mapping
        restMappingMockMvc.perform(post("/api/mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(mapping)))
            .andExpect(status().isCreated());

        // Validate the Mapping in the database
        List<Mapping> mappingList = mappingRepository.findAll();
        assertThat(mappingList).hasSize(databaseSizeBeforeCreate + 1);
        Mapping testMapping = mappingList.get(mappingList.size() - 1);
        assertThat(testMapping.getSvnDirectory()).isEqualTo(DEFAULT_SVN_DIRECTORY);
        assertThat(testMapping.getRegex()).isEqualTo(DEFAULT_REGEX);
        assertThat(testMapping.getGitDirectory()).isEqualTo(DEFAULT_GIT_DIRECTORY);
        assertThat(testMapping.isSvnDirectoryDelete()).isEqualTo(DEFAULT_SVN_DIRECTORY_DELETE);
    }

    @Test
    @Transactional
    public void createMappingWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = mappingRepository.findAll().size();

        // Create the Mapping with an existing ID
        mapping.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restMappingMockMvc.perform(post("/api/mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(mapping)))
            .andExpect(status().isBadRequest());

        // Validate the Mapping in the database
        List<Mapping> mappingList = mappingRepository.findAll();
        assertThat(mappingList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllMappings() throws Exception {
        // Initialize the database
        mappingRepository.saveAndFlush(mapping);

        // Get all the mappingList
        restMappingMockMvc.perform(get("/api/mappings?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(mapping.getId().intValue())))
            .andExpect(jsonPath("$.[*].svnDirectory").value(hasItem(DEFAULT_SVN_DIRECTORY.toString())))
            .andExpect(jsonPath("$.[*].regex").value(hasItem(DEFAULT_REGEX.toString())))
            .andExpect(jsonPath("$.[*].gitDirectory").value(hasItem(DEFAULT_GIT_DIRECTORY.toString())))
            .andExpect(jsonPath("$.[*].svnDirectoryDelete").value(hasItem(DEFAULT_SVN_DIRECTORY_DELETE.booleanValue())));
    }
    
    @Test
    @Transactional
    public void getMapping() throws Exception {
        // Initialize the database
        mappingRepository.saveAndFlush(mapping);

        // Get the mapping
        restMappingMockMvc.perform(get("/api/mappings/{id}", mapping.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(mapping.getId().intValue()))
            .andExpect(jsonPath("$.svnDirectory").value(DEFAULT_SVN_DIRECTORY.toString()))
            .andExpect(jsonPath("$.regex").value(DEFAULT_REGEX.toString()))
            .andExpect(jsonPath("$.gitDirectory").value(DEFAULT_GIT_DIRECTORY.toString()))
            .andExpect(jsonPath("$.svnDirectoryDelete").value(DEFAULT_SVN_DIRECTORY_DELETE.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingMapping() throws Exception {
        // Get the mapping
        restMappingMockMvc.perform(get("/api/mappings/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateMapping() throws Exception {
        // Initialize the database
        mappingService.save(mapping);

        int databaseSizeBeforeUpdate = mappingRepository.findAll().size();

        // Update the mapping
        Mapping updatedMapping = mappingRepository.findById(mapping.getId()).get();
        // Disconnect from session so that the updates on updatedMapping are not directly saved in db
        em.detach(updatedMapping);
        updatedMapping
            .svnDirectory(UPDATED_SVN_DIRECTORY)
            .regex(UPDATED_REGEX)
            .gitDirectory(UPDATED_GIT_DIRECTORY)
            .svnDirectoryDelete(UPDATED_SVN_DIRECTORY_DELETE);

        restMappingMockMvc.perform(put("/api/mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedMapping)))
            .andExpect(status().isOk());

        // Validate the Mapping in the database
        List<Mapping> mappingList = mappingRepository.findAll();
        assertThat(mappingList).hasSize(databaseSizeBeforeUpdate);
        Mapping testMapping = mappingList.get(mappingList.size() - 1);
        assertThat(testMapping.getSvnDirectory()).isEqualTo(UPDATED_SVN_DIRECTORY);
        assertThat(testMapping.getRegex()).isEqualTo(UPDATED_REGEX);
        assertThat(testMapping.getGitDirectory()).isEqualTo(UPDATED_GIT_DIRECTORY);
        assertThat(testMapping.isSvnDirectoryDelete()).isEqualTo(UPDATED_SVN_DIRECTORY_DELETE);
    }

    @Test
    @Transactional
    public void updateNonExistingMapping() throws Exception {
        int databaseSizeBeforeUpdate = mappingRepository.findAll().size();

        // Create the Mapping

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restMappingMockMvc.perform(put("/api/mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(mapping)))
            .andExpect(status().isBadRequest());

        // Validate the Mapping in the database
        List<Mapping> mappingList = mappingRepository.findAll();
        assertThat(mappingList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteMapping() throws Exception {
        // Initialize the database
        mappingService.save(mapping);

        int databaseSizeBeforeDelete = mappingRepository.findAll().size();

        // Get the mapping
        restMappingMockMvc.perform(delete("/api/mappings/{id}", mapping.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Mapping> mappingList = mappingRepository.findAll();
        assertThat(mappingList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Mapping.class);
        Mapping mapping1 = new Mapping();
        mapping1.setId(1L);
        Mapping mapping2 = new Mapping();
        mapping2.setId(mapping1.getId());
        assertThat(mapping1).isEqualTo(mapping2);
        mapping2.setId(2L);
        assertThat(mapping1).isNotEqualTo(mapping2);
        mapping1.setId(null);
        assertThat(mapping1).isNotEqualTo(mapping2);
    }
}
