package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.domain.Mapping;
import fr.yodamad.svn2git.domain.Migration;
import fr.yodamad.svn2git.domain.MigrationHistory;
import fr.yodamad.svn2git.domain.WorkUnit;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;
import fr.yodamad.svn2git.repository.MappingRepository;
import fr.yodamad.svn2git.service.util.GitlabAdmin;
import fr.yodamad.svn2git.service.util.MigrationConstants;
import fr.yodamad.svn2git.service.util.Shell;
import net.logstash.logback.encoder.org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.yodamad.svn2git.service.util.MigrationConstants.*;
import static fr.yodamad.svn2git.service.util.Shell.execCommand;
import static fr.yodamad.svn2git.service.util.Shell.isWindows;
import static java.lang.String.format;
import static java.nio.file.Files.walk;

/**
 * Git operations manager
 */
@Service
public class GitManager {

    private static final Logger LOG = LoggerFactory.getLogger(GitManager.class);

    private GitlabAdmin gitlab;
    // Manager & repository
    private final HistoryManager historyMgr;
    private final MappingRepository mappingRepository;
    private final ApplicationProperties applicationProperties;

    public GitManager(final GitlabAdmin gitlab,
                      final HistoryManager historyManager,
                      final MappingRepository mappingRepository,
                      final ApplicationProperties applicationProperties) {
        this.gitlab = gitlab;
        this.historyMgr = historyManager;
        this.mappingRepository = mappingRepository;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Create project in GitLab
     * @param migration
     * @throws GitLabApiException
     */
    public String createGitlabProject(Migration migration) throws GitLabApiException {
        MigrationHistory history = historyMgr.startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, migration.getGitlabUrl() + migration.getGitlabGroup());

        GitlabAdmin gitlabAdmin = gitlab;
        if (!applicationProperties.gitlab.url.equalsIgnoreCase(migration.getGitlabUrl())
            || !StringUtils.isEmpty(migration.getGitlabToken())) {
            GitLabApi api = new GitLabApi(migration.getGitlabUrl(), migration.getGitlabToken());
            gitlabAdmin.setGitLabApi(api);
        }
        try {
            Group group = gitlabAdmin.groupApi().getGroup(migration.getGitlabGroup());

            // If no svn project specified, use svn group instead
            if (StringUtils.isEmpty(migration.getSvnProject())) {
                gitlabAdmin.projectApi().createProject(group.getId(), migration.getSvnGroup());
                historyMgr.endStep(history, StatusEnum.DONE, null);
                return migration.getSvnGroup();
            } else {
                // split svn structure to create gitlab elements (group(s), project)
                String[] structure = migration.getSvnProject().split("/");
                Integer groupId = group.getId();
                if (structure.length > 2) {
                    for (int module = 1; module < structure.length - 2; module++) {
                        Group gitlabSubGroup = new Group();
                        gitlabSubGroup.setName(structure[module]);
                        gitlabSubGroup.setPath(structure[module]);
                        gitlabSubGroup.setParentId(groupId);
                        groupId = gitlabAdmin.groupApi().addGroup(gitlabSubGroup).getId();
                    }
                }

                Optional<Project> project = gitlabAdmin.groupApi().getProjects(groupId)
                    .stream()
                    .filter(p -> p.getName().equalsIgnoreCase(structure[structure.length - 1]))
                    .findFirst();
                if (!project.isPresent()) {
                    gitlabAdmin.projectApi().createProject(groupId, structure[structure.length - 1]);
                    historyMgr.endStep(history, StatusEnum.DONE, null);
                    return structure[structure.length - 1];
                } else {
                    throw new GitLabApiException("Please remove the destination project '"+group.getName()+"/"+structure[structure.length - 1]);
                }
            }
        } catch (GitLabApiException exc) {
            historyMgr.endStep(history, StatusEnum.FAILED, exc.getMessage());
            throw exc;
        }
    }

    /**
     * Git svn clone command to copy svn as git repository
     * @param workUnit
     * @throws IOException
     * @throws InterruptedException
     */
    public void gitSvnClone(WorkUnit workUnit) throws IOException, InterruptedException {
        String cloneCommand;
        String safeCommand;
        if (StringUtils.isEmpty(workUnit.migration.getSvnUser())) {
            cloneCommand = format("git svn clone --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnUrl(),
                workUnit.migration.getSvnGroup());
            safeCommand = cloneCommand;
        } else {
            String escapedPassword = StringEscapeUtils.escapeJava(workUnit.migration.getSvnPassword());
            cloneCommand = format("echo %s | git svn clone --username %s --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                escapedPassword,
                workUnit.migration.getSvnUser(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnUrl(),
                workUnit.migration.getSvnGroup());
            safeCommand = format("echo %s | git svn clone --username %s --trunk=%s/trunk --branches=%s/branches --tags=%s/tags %s/%s",
                STARS,
                workUnit.migration.getSvnUser(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnProject(),
                workUnit.migration.getSvnUrl(),
                workUnit.migration.getSvnGroup());
        }

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_CHECKOUT, safeCommand);
        execCommand(workUnit.root, cloneCommand, safeCommand);
        historyMgr.endStep(history, StatusEnum.DONE, null);
    }

    /**
     * List SVN branches & tags cloned
     * @param directory working directory
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    public static List<String> branchList(String directory) throws InterruptedException, IOException {
        String command = "git branch -r";
        ProcessBuilder builder = new ProcessBuilder();
        if (Shell.isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }
        builder.directory(new File(directory));

        Process p = builder.start();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        List<String> remotes = new ArrayList<>();
        reader.lines().iterator().forEachRemaining(remotes::add);

        p.waitFor();
        p.destroy();

        return remotes;
    }

    /**
     * Apply mappings configured
     * @param workUnit
     * @param branch Branch to process
     */
    public boolean applyMapping(WorkUnit workUnit, String branch) {
        List<Mapping> mappings = mappingRepository.findAllByMigration(workUnit.migration.getId());
        boolean workDone = false;
        List<StatusEnum> results = null;
        if (!CollectionUtils.isEmpty(mappings)) {
            // Extract mappings with regex
            List<Mapping> regexMappings = mappings.stream()
                .filter(mapping -> !StringUtils.isEmpty(mapping.getRegex()) && !"*".equals(mapping.getRegex()))
                .collect(Collectors.toList());
            results = regexMappings.stream()
                .map(mapping -> mvRegex(workUnit, mapping, branch))
                .collect(Collectors.toList());

            // Remove regex mappings
            mappings.removeAll(regexMappings);
            results.addAll(
                mappings.stream()
                    .map(mapping -> mvDirectory(workUnit, mapping, branch))
                    .collect(Collectors.toList()));
            workDone = results.contains(StatusEnum.DONE);
        }

        if (workDone) {
            MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, format("Push moved elements on %s", branch));
            try {
                // git commit
                String gitCommand = "git add .";
                execCommand(workUnit.directory, gitCommand);
                gitCommand = format("git commit -m \"Apply mappings on %s\"", branch);
                execCommand(workUnit.directory, gitCommand);
                // git push
                execCommand(workUnit.directory, MigrationConstants.GIT_PUSH);

                historyMgr.endStep(history, StatusEnum.DONE, null);
            } catch (IOException | InterruptedException iEx) {
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.getMessage());
                return false;
            }
        }

        // No mappings, OK
        if (results == null) {
            return true;
        }
        // Some errors, WARNING to be set
        return results.contains(StatusEnum.DONE_WITH_WARNINGS);
    }

    /**
     * Apply git mv
     * @param workUnit
     * @param mapping Mapping to apply
     * @param branch Current branch
     */
    private StatusEnum mvDirectory(WorkUnit workUnit, Mapping mapping, String branch) {
        MigrationHistory history;
        String msg = format("git mv %s %s on %s", mapping.getSvnDirectory(), mapping.getGitDirectory(), branch);
        try {
            if (mapping.getGitDirectory().equals("/") || mapping.getGitDirectory().equals(".")) {
                // For root directory, we need to loop for subdirectory
                List<StatusEnum> results = Files.list(Paths.get(workUnit.directory, mapping.getSvnDirectory()))
                    .map(d -> mv(workUnit, format("%s/%s", mapping.getSvnDirectory(), d.getFileName().toString()), d.getFileName().toString(), branch))
                    .collect(Collectors.toList());

                if (results.isEmpty()) {
                    history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);
                    historyMgr.endStep(history, StatusEnum.IGNORED, null);
                    return StatusEnum.IGNORED;
                }

                if (results.contains(StatusEnum.DONE_WITH_WARNINGS)) {
                    return StatusEnum.DONE_WITH_WARNINGS;
                }
                return StatusEnum.DONE;

            } else {
                return mv(workUnit, mapping.getSvnDirectory(), mapping.getGitDirectory(), branch);
            }
        } catch (IOException gitEx) {
            LOG.debug("Failed to mv directory", gitEx);
            history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);
            historyMgr.endStep(history, StatusEnum.IGNORED, null);
            return StatusEnum.IGNORED;
        }
    }

    /**
     * Apply git mv
     * @param workUnit
     * @param mapping Mapping to apply
     * @param branch Current branch
     */
    private StatusEnum mvRegex(WorkUnit workUnit, Mapping mapping, String branch) {
        String msg = format("git mv %s %s based on regex %s on %s", mapping.getSvnDirectory(), mapping.getGitDirectory(), mapping.getRegex(), branch);
        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);

        String regex = mapping.getRegex();
        if (mapping.getRegex().startsWith("*")) { regex = '.' + mapping.getRegex(); }

        Pattern p = Pattern.compile(regex);
        try {
            Path fullPath = Paths.get(workUnit.directory, mapping.getSvnDirectory());
            long result = walk(fullPath)
                .map(Path::toString)
                .filter(s -> !s.equals(fullPath.toString()))
                .map(s -> s.substring(workUnit.directory.length()))
                .map(p::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(0))
                .mapToInt(el -> {
                    try {
                        Path gitPath;
                        if (new File(el).getParentFile() == null) {
                            gitPath = Paths.get(workUnit.directory, mapping.getGitDirectory());
                        } else {
                            gitPath = Paths.get(workUnit.directory, mapping.getGitDirectory(), new File(el).getParent());
                        }

                        if (!Files.exists(gitPath)) {
                            Files.createDirectories(gitPath);
                        }
                        return execCommand(workUnit.directory, format("git mv %s %s", el, Paths.get(mapping.getGitDirectory(), el).toString()));
                    } catch (InterruptedException | IOException e) {
                        return MigrationConstants.ERROR_CODE;
                    }
                }).sum();

            if (result > 0) {
                historyMgr.endStep(history, StatusEnum.DONE_WITH_WARNINGS, null);
                return StatusEnum.DONE_WITH_WARNINGS;
            } else {
                historyMgr.endStep(history, StatusEnum.DONE, null);
                return StatusEnum.DONE;
            }
        } catch (IOException ioEx) {
            historyMgr.endStep(history, StatusEnum.FAILED, ioEx.getMessage());
            return StatusEnum.DONE_WITH_WARNINGS;
        }
    }

    /**
     * Apply git mv
     * @param workUnit
     * @param svnDir Origin SVN element
     * @param gitDir Target Git element
     * @param branch Current branch
     */
    private StatusEnum mv(WorkUnit workUnit, String svnDir, String gitDir, String branch) {
        MigrationHistory history = null;
        try {
            String gitCommand = format("git mv %s %s on %s", svnDir, gitDir, branch);
            history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, gitCommand);
            // git mv
            int exitCode = execCommand(workUnit.directory, gitCommand);

            if (MigrationConstants.ERROR_CODE == exitCode) {
                historyMgr.endStep(history, StatusEnum.IGNORED, null);
                return StatusEnum.IGNORED;
            } else {
                historyMgr.endStep(history, StatusEnum.DONE, null);
                return StatusEnum.DONE;
            }
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to mv directory", gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
            return StatusEnum.DONE_WITH_WARNINGS;
        }
    }

    /**
     * Manage branches extracted from SVN
     * @param workUnit
     * @param remotes
     */
    public void manageBranches(WorkUnit workUnit, List<String> remotes) {
        List<String> gitBranches = remotes.stream()
            .map(String::trim)
            // Remove tags
            .filter(b -> !b.startsWith(ORIGIN_TAGS))
            // Remove master/trunk
            .filter(b -> !b.contains(MASTER))
            .filter(b -> !b.contains("trunk"))
            .collect(Collectors.toList());

        gitBranches.forEach(b -> {
                final boolean warn = pushBranch(workUnit, b);
                workUnit.warnings.set(workUnit.warnings.get() || warn);
            }
        );
    }

    /**
     * Push a branch
     * @param workUnit
     * @param branch Branch to migrate
     */
    public boolean pushBranch(WorkUnit workUnit, String branch) throws RuntimeException {
        String branchName = branch.replaceFirst("refs/remotes/origin/", "");
        branchName = branchName.replaceFirst("origin/", "");
        LOG.debug(format("Branch %s", branchName));


            if (workUnit.migration.getSvnHistory().equals("all")) {
                MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, branchName);
                try {
                    String gitCommand = format("git checkout -b %s %s", branchName, branch);
                    execCommand(workUnit.directory, gitCommand);
                    execCommand(workUnit.directory, MigrationConstants.GIT_PUSH);

                    historyMgr.endStep(history, StatusEnum.DONE, null);
                } catch (IOException | InterruptedException iEx) {
                    LOG.error("Failed to push branch", iEx);
                    historyMgr.endStep(history, StatusEnum.FAILED, iEx.getMessage());
                    return false;
                }
            } else {
                removeHistory(workUnit, branchName);
            }
            return applyMapping(workUnit, branch);
    }

    /**
     * Manage tags extracted from SVN
     * @param workUnit
     * @param remotes
     */
    public void manageTags(WorkUnit workUnit, List<String> remotes) {
        List<String> gitTags = remotes.stream()
            .map(String::trim)
            // Only tags
            .filter(b -> b.startsWith(ORIGIN_TAGS))
            // Remove temp tags
            .filter(b -> !b.contains("@"))
            .collect(Collectors.toList());

        gitTags.forEach(t -> {
                final boolean warn = pushTag(workUnit, t);
                workUnit.warnings.set(workUnit.warnings.get() || warn);
            }
        );
    }

    /**
     * Push a tag
     * @param workUnit
     * @param tag Tag to migrate
     */
    public boolean pushTag(WorkUnit workUnit, String tag) {
        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, tag);
        try {
            String tagName = tag.replaceFirst(ORIGIN_TAGS, "");
            LOG.debug(format("Tag %s", tagName));

            String gitCommand = format("git checkout -b tmp_tag %s", tag);
            execCommand(workUnit.directory, gitCommand);

            gitCommand = "git checkout master";
            execCommand(workUnit.directory, gitCommand);

            gitCommand = format("git tag %s tmp_tag", tagName);

            execCommand(workUnit.directory, gitCommand);

            gitCommand = format("git push -u origin %s", tagName);
            execCommand(workUnit.directory, gitCommand);

            gitCommand = "git branch -D tmp_tag";
            execCommand(workUnit.directory, gitCommand);

            historyMgr.endStep(history, StatusEnum.DONE, null);
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to push branch", gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Remove commit history on a given branch
     * @param workUnit Current work unit
     * @param branch Branch to work on
     */
    public void removeHistory(WorkUnit workUnit, String branch) {
        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, branch);
        try {
            LOG.debug(format("Remove history on %s", branch));

            String gitCommand = "git checkout --orphan TEMP_BRANCH";
            execCommand(workUnit.directory, gitCommand);

            gitCommand = "git add -A";
            execCommand(workUnit.directory, gitCommand);

            try {
                gitCommand = format("git commit -am \"Reset history on %s\"", branch);
                execCommand(workUnit.directory, gitCommand);
            } catch (RuntimeException ex) {
                // Ignored failed step
            }

            try {
                gitCommand = format("git branch -D %s", branch);
                execCommand(workUnit.directory, gitCommand);
            } catch (RuntimeException ex) {
                if (ex.getMessage().equalsIgnoreCase("1")) {
                    // Ignored failed step
                }
            }

            gitCommand = format("git branch -m %s", branch);
            execCommand(workUnit.directory, gitCommand);

            gitCommand = format("git push -f origin %s", branch);
            execCommand(workUnit.directory, gitCommand);

            historyMgr.endStep(history, StatusEnum.DONE, format("Push %s with no history", branch));
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to push branch", gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
        }
    }
}
