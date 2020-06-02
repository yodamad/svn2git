package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.security.AuthoritiesConstants;
import fr.yodamad.svn2git.security.SecurityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for the GitManager class.
 *
 * @see GitManager
 */
public class GitManagerTest {

    private static final Logger log = LoggerFactory.getLogger(GitManagerTest.class);

    // ###### BRANCH AND TAG TESTS ######

    @Test
    public void testIgnoreRefsGenerationProvidedBranchesAndTags() {

        String branchesToMigrate = "branch1";
        String tagsToMigrate = "tag1";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchANDTagProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1|tags/tag1).*$\"");
    }

    @Test
    public void testIgnoreRefsGenerationProvided2BranchesAnd2Tags() {

        String branchesToMigrate = "branch1,branch2";
        String tagsToMigrate = "tag1,tag2";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchANDTagProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1|branch2|tags/tag1|tags/tag2).*$\"");
    }

    @Test
    public void testIgnoreRefsGenerationProvided2BranchesAnd2TagsWithSpaces() {

        String branchesToMigrate = " branch1 , branch2 ";
        String tagsToMigrate = " tag1 , tag2 ";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchANDTagProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1|branch2|tags/tag1|tags/tag2).*$\"");
    }

    @Test
    public void testIgnoreRefsGenerationProvided2BranchesAnd2TagsWithSpacesAndFullStops() {

        String branchesToMigrate = " branch1.1 , branch2.1 ";
        String tagsToMigrate = " tag1 , tag2.2 ";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchANDTagProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1\\.1|branch2\\.1|tags/tag1|tags/tag2\\.2).*$\"");
    }

    // ###### BRANCH ONLY TESTS ######

    @Test
    public void testIgnoreRefsGenerationProvidedBranchesOnly() {

        String branchesToMigrate = "branch1";
        String tagsToMigrate = "";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchONLYProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1|tags/).*$\"");

    }

    @Test
    public void testIgnoreRefsGenerationProvided3BranchesOnly() {

        String branchesToMigrate = "branch1,branch2,branch3";
        String tagsToMigrate = "";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchONLYProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1|branch2|branch3|tags/).*$\"");

    }

    @Test
    public void testIgnoreRefsGenerationProvided3BranchesOnlyWithSpacesAndFullStops() {

        String branchesToMigrate = "branch1,  branch2, branch4.1    ";
        String tagsToMigrate = "  ";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("BranchONLYProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/(?!branch1|branch2|branch4\\.1|tags/).*$\"");

    }

    // ###### TAG ONLY TESTS ######

    @Test
    public void testIgnoreRefsGenerationProvidedTagsOnly() {

        String branchesToMigrate = "";
        String tagsToMigrate = "tag1";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("TagONLYProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/tags/(?!tag1).*$\"");

    }

    @Test
    public void testIgnoreRefsGenerationProvided2TagsOnly() {

        String branchesToMigrate = "";
        String tagsToMigrate = "tag1,tag4.1";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("TagONLYProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/tags/(?!tag1|tag4\\.1).*$\"");

    }

    @Test
    public void testIgnoreRefsGenerationProvided2TagsOnlyWithSpaces() {

        String branchesToMigrate = "     ";
        String tagsToMigrate = "  tag1 ,      tag4.1   ";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("TagONLYProvided: %s", regex));

        assertThat(regex).isEqualTo("--ignore-refs=\"^refs/remotes/origin/tags/(?!tag1|tag4\\.1).*$\"");

    }

    // ###### NO BRANCHES NO TAGS TESTS ######

    @Test
    public void testIgnoreRefsGenerationNoBranchesNoTags() {

        String branchesToMigrate = "";
        String tagsToMigrate = "";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("NoBranchNoTagProvided: %s", regex));

        assertThat(regex).isEqualTo("");

    }

    @Test
    public void testIgnoreRefsGenerationNoBranchesNoTagsWithSpaces() {

        String branchesToMigrate = "         ";
        String tagsToMigrate = "  ";

        String regex = GitManager.generateIgnoreRefs(branchesToMigrate, tagsToMigrate);
        log.info(format("NoBranchNoTagProvided: %s", regex));

        assertThat(regex).isEqualTo("");

    }

}
