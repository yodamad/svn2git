package fr.yodamad.svn2git.web.rest;

import fr.yodamad.svn2git.Svn2GitApp;
import fr.yodamad.svn2git.data.Repository;
import fr.yodamad.svn2git.domain.SvnInfo;
import fr.yodamad.svn2git.domain.SvnStructure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.tmatesoft.svn.core.SVNAuthenticationException;

import java.util.List;

import static fr.yodamad.svn2git.data.Repository.Modules.MODULE_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = Svn2GitApp.class)
public class SvnResourceTest {

    @Autowired
    private SvnResource svnResource;

    public static final Integer DEPTH = 2;

    public static SvnInfo svnInfo = new SvnInfo();

    @BeforeEach
    public void initSvnInfo() {
        svnInfo = new SvnInfo();
        svnInfo.url = "https://chaos.yodamad.fr/svn";
    }

    @Test
    public void test_svn_listing_on_simple_repo() {
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, Repository.simple().name, DEPTH);
        assertThat(svnStructure.modules).isEmpty();
        assertThat(svnStructure.flat).isTrue();
        assertThat(svnStructure.root).isTrue();
    }

    @Test
    public void test_svn_listing_with_overriden_credz() {
        svnInfo.user = "demo";
        svnInfo.password = "demo";
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, Repository.simple().name, DEPTH);
        assertThat(svnStructure.modules).isEmpty();
        assertThat(svnStructure.flat).isTrue();
        assertThat(svnStructure.root).isTrue();
    }

    @Test
    public void test_svn_listing_on_flat_repo() {
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, Repository.flat().name, DEPTH);
        assertThat(svnStructure.modules).isEmpty();
        assertThat(svnStructure.flat).isTrue();
        assertThat(svnStructure.root).isFalse();
    }

    @Test
    public void test_svn_listing_on_complex_repo() {
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, Repository.complex().namespace, DEPTH);
        assertThat(svnStructure.modules).isNotEmpty();
        assertThat(svnStructure.flat).isFalse();
        List<SvnStructure.SvnModule> modules = svnStructure.modules;
        assertThat(modules.size()).isEqualTo(3);
        modules.forEach(
            m -> {
                assertThat(m.name).isIn(Repository.ALL_MODULES);
                if (MODULE_1.equals(m.name)) {
                    assertThat(m.subModules).isNotEmpty();
                    assertThat(m.subModules.size()).isEqualTo(2);
                }
            }
        );
    }

    @Test
    public void test_svn_listing_on_mixed_repo() {
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, "mixed", DEPTH);
        assertThat(svnStructure.modules).isNotEmpty();
        assertThat(svnStructure.flat).isFalse();
        List<SvnStructure.SvnModule> modules = svnStructure.modules;
        assertThat(modules.size()).isEqualTo(3);
        modules.forEach(
            m -> {
                assertThat(m.name).isIn("complex", "flat", "simple");
                switch (m.name) {
                    case "complex":
                        assertThat(m.subModules).isNotEmpty();
                        assertThat(m.subModules.size()).isEqualTo(2);
                        break;
                    case "flat":
                        assertThat(m.layoutElements).isEmpty();
                        assertThat(m.flat).isTrue();
                        break;
                    case "simple":
                        assertThat(m.layoutElements).isNotEmpty();
                        assertThat(m.flat).isFalse();
                        break;
                    default:
                        break;
                }
            }
        );
    }

    @Test
    public void test_svn_listing_with_no_depth() {
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, Repository.complex().namespace, 0);
        assertThat(svnStructure.modules).isEmpty();
        assertThat(svnStructure.flat).isTrue();
    }

    @Test
    public void test_svn_listing_with_depth_1() {
        SvnStructure svnStructure = svnResource.listSVN(svnInfo, Repository.complex().namespace, 1);
        assertThat(svnStructure.modules).isNotEmpty();
        assertThat(svnStructure.flat).isFalse();
        List<SvnStructure.SvnModule> modules = svnStructure.modules;
        assertThat(modules.size()).isEqualTo(3);
        modules.forEach(m -> {
            assertThat(m.subModules).isEmpty();
            assertThat(m.flat).isFalse();
        });
    }

    @Test
    public void test_invalid_credentials() {
        svnInfo.user = "hacker";
        svnInfo.password = "hacker";

        assertThrows(SVNAuthenticationException.class, () -> {
            svnResource.listSVN(svnInfo, Repository.simple().name, DEPTH);
        });
    }
}
