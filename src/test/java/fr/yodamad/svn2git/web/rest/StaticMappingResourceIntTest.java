package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.StaticMapping;
import fr.yodamad.svn2git.repository.StaticMappingRepository;
import fr.yodamad.svn2git.service.StaticMappingService;
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
 * Test class for the StaticMappingResource REST controller.
 *
 * @see StaticMappingResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class StaticMappingResourceIntTest {

    private static final String DEFAULT_SVN_DIRECTORY = "AAAAAAAAAA";
    private static final String UPDATED_SVN_DIRECTORY = "BBBBBBBBBB";

    private static final String DEFAULT_REGEX = "AAAAAAAAAA";
    private static final String UPDATED_REGEX = "BBBBBBBBBB";

    private static final String DEFAULT_GIT_DIRECTORY = "AAAAAAAAAA";
    private static final String UPDATED_GIT_DIRECTORY = "BBBBBBBBBB";

    private static final Boolean DEFAULT_SVN_DIRECTORY_DELETE = false;
    private static final Boolean UPDATED_SVN_DIRECTORY_DELETE = true;

    @Autowired
    private StaticMappingRepository staticMappingRepository;
    
    @Autowired
    private StaticMappingService staticMappingService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restStaticMappingMockMvc;

    private StaticMapping staticMapping;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final StaticMappingResource staticMappingResource = new StaticMappingResource(staticMappingService);
        this.restStaticMappingMockMvc = MockMvcBuilders.standaloneSetup(staticMappingResource)
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
    public static StaticMapping createEntity(EntityManager em) {
        StaticMapping staticMapping = new StaticMapping()
            .svnDirectory(DEFAULT_SVN_DIRECTORY)
            .regex(DEFAULT_REGEX)
            .gitDirectory(DEFAULT_GIT_DIRECTORY)
            .svnDirectoryDelete(DEFAULT_SVN_DIRECTORY_DELETE);
        return staticMapping;
    }

    @Before
    public void initTest() {
        staticMapping = createEntity(em);
    }

    @Test
    @Transactional
    public void createStaticMapping() throws Exception {
        int databaseSizeBeforeCreate = staticMappingRepository.findAll().size();

        // Create the StaticMapping
        restStaticMappingMockMvc.perform(post("/api/static-mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticMapping)))
            .andExpect(status().isCreated());

        // Validate the StaticMapping in the database
        List<StaticMapping> staticMappingList = staticMappingRepository.findAll();
        assertThat(staticMappingList).hasSize(databaseSizeBeforeCreate + 1);
        StaticMapping testStaticMapping = staticMappingList.get(staticMappingList.size() - 1);
        assertThat(testStaticMapping.getSvnDirectory()).isEqualTo(DEFAULT_SVN_DIRECTORY);
        assertThat(testStaticMapping.getRegex()).isEqualTo(DEFAULT_REGEX);
        assertThat(testStaticMapping.getGitDirectory()).isEqualTo(DEFAULT_GIT_DIRECTORY);
        assertThat(testStaticMapping.isSvnDirectoryDelete()).isEqualTo(DEFAULT_SVN_DIRECTORY_DELETE);
    }

    @Test
    @Transactional
    public void createStaticMappingWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = staticMappingRepository.findAll().size();

        // Create the StaticMapping with an existing ID
        staticMapping.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStaticMappingMockMvc.perform(post("/api/static-mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticMapping)))
            .andExpect(status().isBadRequest());

        // Validate the StaticMapping in the database
        List<StaticMapping> staticMappingList = staticMappingRepository.findAll();
        assertThat(staticMappingList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllStaticMappings() throws Exception {
        // Initialize the database
        staticMappingRepository.saveAndFlush(staticMapping);

        // Get all the staticMappingList
        restStaticMappingMockMvc.perform(get("/api/static-mappings?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(staticMapping.getId().intValue())))
            .andExpect(jsonPath("$.[*].svnDirectory").value(hasItem(DEFAULT_SVN_DIRECTORY.toString())))
            .andExpect(jsonPath("$.[*].regex").value(hasItem(DEFAULT_REGEX.toString())))
            .andExpect(jsonPath("$.[*].gitDirectory").value(hasItem(DEFAULT_GIT_DIRECTORY.toString())))
            .andExpect(jsonPath("$.[*].svnDirectoryDelete").value(hasItem(DEFAULT_SVN_DIRECTORY_DELETE.booleanValue())));
    }
    
    @Test
    @Transactional
    public void getStaticMapping() throws Exception {
        // Initialize the database
        staticMappingRepository.saveAndFlush(staticMapping);

        // Get the staticMapping
        restStaticMappingMockMvc.perform(get("/api/static-mappings/{id}", staticMapping.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(staticMapping.getId().intValue()))
            .andExpect(jsonPath("$.svnDirectory").value(DEFAULT_SVN_DIRECTORY.toString()))
            .andExpect(jsonPath("$.regex").value(DEFAULT_REGEX.toString()))
            .andExpect(jsonPath("$.gitDirectory").value(DEFAULT_GIT_DIRECTORY.toString()))
            .andExpect(jsonPath("$.svnDirectoryDelete").value(DEFAULT_SVN_DIRECTORY_DELETE.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingStaticMapping() throws Exception {
        // Get the staticMapping
        restStaticMappingMockMvc.perform(get("/api/static-mappings/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateStaticMapping() throws Exception {
        // Initialize the database
        staticMappingService.save(staticMapping);

        int databaseSizeBeforeUpdate = staticMappingRepository.findAll().size();

        // Update the staticMapping
        StaticMapping updatedStaticMapping = staticMappingRepository.findById(staticMapping.getId()).get();
        // Disconnect from session so that the updates on updatedStaticMapping are not directly saved in db
        em.detach(updatedStaticMapping);
        updatedStaticMapping
            .svnDirectory(UPDATED_SVN_DIRECTORY)
            .regex(UPDATED_REGEX)
            .gitDirectory(UPDATED_GIT_DIRECTORY)
            .svnDirectoryDelete(UPDATED_SVN_DIRECTORY_DELETE);

        restStaticMappingMockMvc.perform(put("/api/static-mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedStaticMapping)))
            .andExpect(status().isOk());

        // Validate the StaticMapping in the database
        List<StaticMapping> staticMappingList = staticMappingRepository.findAll();
        assertThat(staticMappingList).hasSize(databaseSizeBeforeUpdate);
        StaticMapping testStaticMapping = staticMappingList.get(staticMappingList.size() - 1);
        assertThat(testStaticMapping.getSvnDirectory()).isEqualTo(UPDATED_SVN_DIRECTORY);
        assertThat(testStaticMapping.getRegex()).isEqualTo(UPDATED_REGEX);
        assertThat(testStaticMapping.getGitDirectory()).isEqualTo(UPDATED_GIT_DIRECTORY);
        assertThat(testStaticMapping.isSvnDirectoryDelete()).isEqualTo(UPDATED_SVN_DIRECTORY_DELETE);
    }

    @Test
    @Transactional
    public void updateNonExistingStaticMapping() throws Exception {
        int databaseSizeBeforeUpdate = staticMappingRepository.findAll().size();

        // Create the StaticMapping

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStaticMappingMockMvc.perform(put("/api/static-mappings")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticMapping)))
            .andExpect(status().isBadRequest());

        // Validate the StaticMapping in the database
        List<StaticMapping> staticMappingList = staticMappingRepository.findAll();
        assertThat(staticMappingList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteStaticMapping() throws Exception {
        // Initialize the database
        staticMappingService.save(staticMapping);

        int databaseSizeBeforeDelete = staticMappingRepository.findAll().size();

        // Get the staticMapping
        restStaticMappingMockMvc.perform(delete("/api/static-mappings/{id}", staticMapping.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<StaticMapping> staticMappingList = staticMappingRepository.findAll();
        assertThat(staticMappingList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StaticMapping.class);
        StaticMapping staticMapping1 = new StaticMapping();
        staticMapping1.setId(1L);
        StaticMapping staticMapping2 = new StaticMapping();
        staticMapping2.setId(staticMapping1.getId());
        assertThat(staticMapping1).isEqualTo(staticMapping2);
        staticMapping2.setId(2L);
        assertThat(staticMapping1).isNotEqualTo(staticMapping2);
        staticMapping1.setId(null);
        assertThat(staticMapping1).isNotEqualTo(staticMapping2);
    }
}
