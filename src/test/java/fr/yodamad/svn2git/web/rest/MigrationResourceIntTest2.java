package fr.yodamad.svn2git.web.rest;

import com.jayway.jsonpath.JsonPath;
import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.repository.MigrationRepository;
import fr.yodamad.svn2git.service.MappingService;
import fr.yodamad.svn2git.service.MigrationHistoryService;
import fr.yodamad.svn2git.service.MigrationManager;
import fr.yodamad.svn2git.web.rest.errors.ExceptionTranslator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.File;
import java.nio.file.Files;

import static fr.yodamad.svn2git.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the MigrationResource REST controller.
 *
 * @see MigrationResource
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class MigrationResourceIntTest2 {

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

    @Autowired
    ResourceLoader resourceLoader;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        final MigrationResource migrationResource = new MigrationResource(migrationRepository, migrationManager, migrationHistoryService, mappingService, gitlabResource, applicationProperties);
        this.restMigrationMockMvc = MockMvcBuilders.standaloneSetup(migrationResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Test
    @Transactional
    public void createMigration() throws Exception {
        int databaseSizeBeforeCreate = migrationRepository.findAll().size();

        File migrationJsonFile = resourceLoader.getResource(
            "classpath:migrations/migration1.json").getFile();

        String migrationJsonString = new String(
            Files.readAllBytes(migrationJsonFile.toPath()));

        // Create the Migration
        MvcResult mvcResult = restMigrationMockMvc.perform(post("/api/migrations")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(migrationJsonString))
            .andExpect(status().isCreated()).andReturn();

        // System.out.println(mvcResult.getResponse().getContentAsString());
        Integer id = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.id");

        int databaseSizeAfterCreate = migrationRepository.findAll().size();

        assertThat(databaseSizeAfterCreate).isEqualTo(databaseSizeBeforeCreate + 1);

        // TODO : Would be good to test end to end migration.
        // Note: Some alternative config to deactivate async needed if pass by controller.
        // i) without having to do git svn clone? (e.g. unzip previously git svn clone)
        // ii) without really pushing artifacts to artifactory (but counting the artifacts expected)
        // iii) without really interacting with gitlab. (but counting the tags, etc expected.)

    }


}
