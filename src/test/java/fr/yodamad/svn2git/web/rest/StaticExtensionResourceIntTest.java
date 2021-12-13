package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.domain.StaticExtension;
import fr.yodamad.svn2git.repository.StaticExtensionRepository;
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
import java.util.List;

import static fr.yodamad.svn2git.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the StaticExtensionResource REST controller.
 *
 * @see StaticExtensionResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class StaticExtensionResourceIntTest {

    private static final String DEFAULT_VALUE = "AAAAAAAAAA";
    private static final String UPDATED_VALUE = "BBBBBBBBBB";

    private static final String DEFAULT_DESCRIPTION = "AAAAAAAAAA";
    private static final String UPDATED_DESCRIPTION = "BBBBBBBBBB";

    private static final Boolean DEFAULT_ENABLED = false;
    private static final Boolean UPDATED_ENABLED = true;

    @Autowired
    private StaticExtensionRepository staticExtensionRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restStaticExtensionMockMvc;

    private StaticExtension staticExtension;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final StaticExtensionResource staticExtensionResource = new StaticExtensionResource(staticExtensionRepository);
        this.restStaticExtensionMockMvc = MockMvcBuilders.standaloneSetup(staticExtensionResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();

        staticExtension = createEntity(em);
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static StaticExtension createEntity(EntityManager em) {
        StaticExtension staticExtension = new StaticExtension()
            .value(DEFAULT_VALUE)
            .description(DEFAULT_DESCRIPTION)
            .enabled(DEFAULT_ENABLED);
        return staticExtension;
    }

    @Test
    @Transactional
    public void createStaticExtension() throws Exception {
        int databaseSizeBeforeCreate = staticExtensionRepository.findAll().size();

        // Create the StaticExtension
        restStaticExtensionMockMvc.perform(post("/api/static-extensions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticExtension)))
            .andExpect(status().isCreated());

        // Validate the StaticExtension in the database
        List<StaticExtension> staticExtensionList = staticExtensionRepository.findAll();
        assertThat(staticExtensionList).hasSize(databaseSizeBeforeCreate + 1);
        StaticExtension testStaticExtension = staticExtensionList.get(staticExtensionList.size() - 1);
        assertThat(testStaticExtension.getValue()).isEqualTo(DEFAULT_VALUE);
        assertThat(testStaticExtension.getDescription()).isEqualTo(DEFAULT_DESCRIPTION);
        assertThat(testStaticExtension.isEnabled()).isEqualTo(DEFAULT_ENABLED);
    }

    @Test
    @Transactional
    public void createStaticExtensionWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = staticExtensionRepository.findAll().size();

        // Create the StaticExtension with an existing ID
        staticExtension.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restStaticExtensionMockMvc.perform(post("/api/static-extensions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticExtension)))
            .andExpect(status().isBadRequest());

        // Validate the StaticExtension in the database
        List<StaticExtension> staticExtensionList = staticExtensionRepository.findAll();
        assertThat(staticExtensionList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkValueIsRequired() throws Exception {
        int databaseSizeBeforeTest = staticExtensionRepository.findAll().size();
        // set the field null
        staticExtension.setValue(null);

        // Create the StaticExtension, which fails.

        restStaticExtensionMockMvc.perform(post("/api/static-extensions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticExtension)))
            .andExpect(status().isBadRequest());

        List<StaticExtension> staticExtensionList = staticExtensionRepository.findAll();
        assertThat(staticExtensionList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllStaticExtensions() throws Exception {
        // Initialize the database
        staticExtensionRepository.saveAndFlush(staticExtension);

        // Get all the staticExtensionList
        restStaticExtensionMockMvc.perform(get("/api/static-extensions?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(staticExtension.getId().intValue())))
            .andExpect(jsonPath("$.[*].value").value(hasItem(DEFAULT_VALUE.toString())))
            .andExpect(jsonPath("$.[*].description").value(hasItem(DEFAULT_DESCRIPTION.toString())))
            .andExpect(jsonPath("$.[*].enabled").value(hasItem(DEFAULT_ENABLED.booleanValue())));
    }

    @Test
    @Transactional
    public void getStaticExtension() throws Exception {
        // Initialize the database
        staticExtensionRepository.saveAndFlush(staticExtension);

        // Get the staticExtension
        restStaticExtensionMockMvc.perform(get("/api/static-extensions/{id}", staticExtension.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(staticExtension.getId().intValue()))
            .andExpect(jsonPath("$.value").value(DEFAULT_VALUE.toString()))
            .andExpect(jsonPath("$.description").value(DEFAULT_DESCRIPTION.toString()))
            .andExpect(jsonPath("$.enabled").value(DEFAULT_ENABLED.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingStaticExtension() throws Exception {
        // Get the staticExtension
        restStaticExtensionMockMvc.perform(get("/api/static-extensions/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateStaticExtension() throws Exception {
        // Initialize the database
        staticExtensionRepository.saveAndFlush(staticExtension);

        int databaseSizeBeforeUpdate = staticExtensionRepository.findAll().size();

        // Update the staticExtension
        StaticExtension updatedStaticExtension = staticExtensionRepository.findById(staticExtension.getId()).get();
        // Disconnect from session so that the updates on updatedStaticExtension are not directly saved in db
        em.detach(updatedStaticExtension);
        updatedStaticExtension
            .value(UPDATED_VALUE)
            .description(UPDATED_DESCRIPTION)
            .enabled(UPDATED_ENABLED);

        restStaticExtensionMockMvc.perform(put("/api/static-extensions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedStaticExtension)))
            .andExpect(status().isOk());

        // Validate the StaticExtension in the database
        List<StaticExtension> staticExtensionList = staticExtensionRepository.findAll();
        assertThat(staticExtensionList).hasSize(databaseSizeBeforeUpdate);
        StaticExtension testStaticExtension = staticExtensionList.get(staticExtensionList.size() - 1);
        assertThat(testStaticExtension.getValue()).isEqualTo(UPDATED_VALUE);
        assertThat(testStaticExtension.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(testStaticExtension.isEnabled()).isEqualTo(UPDATED_ENABLED);
    }

    @Test
    @Transactional
    public void updateNonExistingStaticExtension() throws Exception {
        int databaseSizeBeforeUpdate = staticExtensionRepository.findAll().size();

        // Create the StaticExtension

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restStaticExtensionMockMvc.perform(put("/api/static-extensions")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(staticExtension)))
            .andExpect(status().isBadRequest());

        // Validate the StaticExtension in the database
        List<StaticExtension> staticExtensionList = staticExtensionRepository.findAll();
        assertThat(staticExtensionList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteStaticExtension() throws Exception {
        // Initialize the database
        staticExtensionRepository.saveAndFlush(staticExtension);

        int databaseSizeBeforeDelete = staticExtensionRepository.findAll().size();

        // Get the staticExtension
        restStaticExtensionMockMvc.perform(delete("/api/static-extensions/{id}", staticExtension.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<StaticExtension> staticExtensionList = staticExtensionRepository.findAll();
        assertThat(staticExtensionList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StaticExtension.class);
        StaticExtension staticExtension1 = new StaticExtension();
        staticExtension1.setId(1L);
        StaticExtension staticExtension2 = new StaticExtension();
        staticExtension2.setId(staticExtension1.getId());
        assertThat(staticExtension1).isEqualTo(staticExtension2);
        staticExtension2.setId(2L);
        assertThat(staticExtension1).isNotEqualTo(staticExtension2);
        staticExtension1.setId(null);
        assertThat(staticExtension1).isNotEqualTo(staticExtension2);
    }
}
