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
import org.gitlab4j.api.models.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.yodamad.svn2git.service.util.MigrationConstants.*;
import static fr.yodamad.svn2git.service.util.Shell.execCommand;
import static java.lang.String.format;
import static java.nio.file.Files.walk;

/**
 * Git operations manager
 */
@Service
public class GitManager {

    private static final Logger LOG = LoggerFactory.getLogger(GitManager.class);
    private static final String FAILED_TO_PUSH_BRANCH = "Failed to push branch";

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
                String currentPath = group.getPath();
                if (structure.length > 2) {
                    for (int module = 1; module < structure.length - 1; module++) {
                        Group gitlabSubGroup = new Group();
                        gitlabSubGroup.setName(structure[module]);
                        gitlabSubGroup.setPath(structure[module]);
                        gitlabSubGroup.setVisibility(Visibility.INTERNAL);
                        currentPath += format("/%s", structure[module]);
                        gitlabSubGroup.setParentId(groupId);
                        try {
                            groupId = gitlabAdmin.groupApi().addGroup(gitlabSubGroup).getId();
                        } catch (GitLabApiException gitlabApiEx) {
                            // Ignore error & get existing groupId
                            groupId = gitlabAdmin.groupApi().getGroup(currentPath).getId();
                            continue;
                        }
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
     * @param workUnit Current work unit
     * @throws IOException
     * @throws InterruptedException
     */
    public void gitSvnClone(WorkUnit workUnit) throws IOException, InterruptedException {
        String cloneCommand;
        String safeCommand;

        if (!StringUtils.isEmpty(workUnit.migration.getSvnPassword())) {
            String escapedPassword = StringEscapeUtils.escapeJava(workUnit.migration.getSvnPassword());
            cloneCommand = initCommand(workUnit, workUnit.migration.getSvnUser(), escapedPassword);
            safeCommand = initCommand(workUnit, workUnit.migration.getSvnUser(), STARS);
        } else if (!StringUtils.isEmpty(applicationProperties.svn.password)) {
            String escapedPassword = StringEscapeUtils.escapeJava(applicationProperties.svn.password);
            cloneCommand = initCommand(workUnit, applicationProperties.svn.user, escapedPassword);
            safeCommand = initCommand(workUnit, applicationProperties.svn.user, STARS);
        } else {
            cloneCommand = initCommand(workUnit, null, null);
            safeCommand = cloneCommand;
        }

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_CHECKOUT, safeCommand);
        execCommand(workUnit.root, cloneCommand, safeCommand);
        historyMgr.endStep(history, StatusEnum.DONE, null);
    }

    /**
     * Init command with or without password in clear
     * @param workUnit Current workUnit
     * @param username Username to use
     * @param secret Escaped password
     * @return
     */
    private static String initCommand(WorkUnit workUnit, String username, String secret) {
        return format("%s git svn clone %s %s %s %s %s/%s",
            StringUtils.isEmpty(secret) ? "" : format("echo %s |", secret),
            StringUtils.isEmpty(username) ? "" : format("--username %s", username),
            workUnit.migration.getTrunk() == null ? "" : format("--trunk=%s/trunk", workUnit.migration.getSvnProject()),
            workUnit.migration.getBranches() == null ? "" : format("--branches=%s/branches", workUnit.migration.getSvnProject()),
            workUnit.migration.getTags() == null ? "" : format("--tags=%s/tags", workUnit.migration.getSvnProject()),
            workUnit.migration.getSvnUrl().endsWith("/") ? workUnit.migration.getSvnUrl() : format("%s/", workUnit.migration.getSvnUrl()),
            workUnit.migration.getSvnGroup());
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
        builder.directory(new File(Shell.formatDirectory(directory)));

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
                gitCommand = format("%s --set-upstream origin %s", GIT_PUSH, branch);
                execCommand(workUnit.directory, gitCommand);

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

        // If git directory in mapping contains /, we need to create root directories must be manually created
        if (mapping.getGitDirectory().contains("/") && !mapping.getGitDirectory().equals("/")) {
            AtomicReference<Path> tmpPath = new AtomicReference<>(Paths.get(workUnit.directory));
            Arrays.stream(mapping.getGitDirectory().split("/"))
                .forEach(dir -> {
                    Path newPath = Paths.get(tmpPath.toString(), dir);
                    if (!Files.exists(newPath)) {
                        try {
                            Files.createDirectory(newPath);
                        } catch (IOException ioEx) {
                            ioEx.printStackTrace();
                        }
                    }
                    tmpPath.set(newPath);
                });
        }

        try (Stream<Path> files = Files.list(Paths.get(workUnit.directory, mapping.getSvnDirectory()))) {
            if (mapping.getGitDirectory().equals("/") || mapping.getGitDirectory().equals(".") || mapping.getGitDirectory().contains("/")) {
                history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);
                StatusEnum result = StatusEnum.DONE;
                boolean useGitDir = mapping.getGitDirectory().contains("/");
                // For root directory, we need to loop for subdirectory
                List<StatusEnum> results = files
                    .map(d -> mv(workUnit, 
                        format("%s/%s", mapping.getSvnDirectory(), d.getFileName().toString()), 
                        useGitDir ? mapping.getGitDirectory() : d.getFileName().toString(), 
                        branch, false))
                    .collect(Collectors.toList());

                if (results.isEmpty()) {
                    result = StatusEnum.IGNORED;
                }

                if (results.contains(StatusEnum.DONE_WITH_WARNINGS)) {
                    result = StatusEnum.DONE_WITH_WARNINGS;
                }
                historyMgr.endStep(history, result, null);
                return result;
                
            } else {
                return mv(workUnit, mapping.getSvnDirectory(), mapping.getGitDirectory(), branch, true);
            }
        } catch (IOException gitEx) {
            history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);
            if (gitEx instanceof NoSuchFileException) {
                historyMgr.endStep(history, StatusEnum.IGNORED, null);
            } else {
                historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
            }

            return StatusEnum.IGNORED;
        }
    }
    
    /**
     * Apply git mv
     * @param workUnit Current work unit
     * @param mapping Mapping to apply
     * @param branch Current branch
     */
    private StatusEnum mvRegex(WorkUnit workUnit, Mapping mapping, String branch) {
        String msg = format("git mv %s %s based on regex %s on %s", mapping.getSvnDirectory(), mapping.getGitDirectory(), mapping.getRegex(), branch);
        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);

        String regex = mapping.getRegex();
        if (mapping.getRegex().startsWith("*")) { regex = '.' + mapping.getRegex(); }

        Pattern p = Pattern.compile(regex);
        Path fullPath = Paths.get(workUnit.directory, mapping.getSvnDirectory());
        try (Stream<Path> walker = walk(fullPath)) {
            long result = walker
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
    private StatusEnum mv(WorkUnit workUnit, String svnDir, String gitDir, String branch, boolean traceStep) {
        MigrationHistory history = null;
        try {
            String historyCommand = format("git mv %s %s on %s", svnDir, gitDir, branch);
            String gitCommand = format("git mv %s %s", svnDir, gitDir);
            if (traceStep) history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, historyCommand);
            // git mv
            int exitCode = execCommand(workUnit.directory, gitCommand);

            if (MigrationConstants.ERROR_CODE == exitCode) {
                if (traceStep) historyMgr.endStep(history, StatusEnum.IGNORED, null);
                return StatusEnum.IGNORED;
            } else {
                if (traceStep) historyMgr.endStep(history, StatusEnum.DONE, null);
                return StatusEnum.DONE;
            }
        } catch (IOException | InterruptedException gitEx) {
            LOG.error("Failed to mv directory", gitEx);
            if (traceStep) historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
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
            .filter(b -> !b.contains("@"))
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

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, branchName);
        String gitCommand = format("git checkout -b %s %s", branchName, branch);
        try {
            execCommand(workUnit.directory, gitCommand);
        } catch (IOException | InterruptedException iEx) {
            LOG.error(FAILED_TO_PUSH_BRANCH, iEx);
            historyMgr.endStep(history, StatusEnum.FAILED, iEx.getMessage());
            return false;
        }

        if (workUnit.migration.getSvnHistory().equals("all")) {
                try {
                    addRemote(workUnit, true);

                    gitCommand = format("%s --set-upstream origin %s", GIT_PUSH, branchName);
                    execCommand(workUnit.directory, gitCommand);
                    historyMgr.endStep(history, StatusEnum.DONE, null);
                } catch (IOException | InterruptedException iEx) {
                    LOG.error(FAILED_TO_PUSH_BRANCH, iEx);
                    historyMgr.endStep(history, StatusEnum.FAILED, iEx.getMessage());
                    return false;
                }
            } else {
                removeHistory(workUnit, branchName, history);
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
     * @param workUnit Current work unit
     * @param tag Tag to migrate
     */
    private boolean pushTag(WorkUnit workUnit, String tag) {
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

            addRemote(workUnit, false);

            gitCommand = format("git push -u origin %s", tagName);
            execCommand(workUnit.directory, gitCommand);

            gitCommand = "git branch -D tmp_tag";
            execCommand(workUnit.directory, gitCommand);

            historyMgr.endStep(history, StatusEnum.DONE, null);
        } catch (IOException | InterruptedException gitEx) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
        }
        return false;
    }

    /**
     * Remove commit history on a given branch
     * @param workUnit Current work unit
     * @param branch Branch to work on
     * @param history Current history instance
     */
    public void removeHistory(WorkUnit workUnit, String branch, MigrationHistory history) {
        try {
            LOG.debug(format("Remove history on %s", branch));

            String gitCommand = format("git checkout --orphan TEMP_BRANCH_%s", branch);
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

            addRemote(workUnit, true);

            gitCommand = format("git push -f origin %s", branch);
            execCommand(workUnit.directory, gitCommand);

            historyMgr.endStep(history, StatusEnum.DONE, format("Push %s with no history", branch));
        } catch (IOException | InterruptedException gitEx) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
        }
    }


    /**
     * Build command to add remote
     * @param workUnit Current work unit
     * @param project Current project
     * @param safeMode safe mode for logs
     * @return
     */
    public String buildRemoteCommand(WorkUnit workUnit, String project, boolean safeMode) {

        if (StringUtils.isEmpty(project)) {
            project = StringUtils.isEmpty(workUnit.migration.getSvnProject()) ?
                workUnit.migration.getSvnGroup()
                : workUnit.migration.getSvnProject();
        }

        URI uri = URI.create(workUnit.migration.getGitlabUrl());
        return format("git remote add origin %s://%s:%s@%s/%s/%s.git",
            uri.getScheme(),
            workUnit.migration.getGitlabToken() == null ?
                applicationProperties.gitlab.account : workUnit.migration.getSvnUser(),
            safeMode ? STARS :
                (workUnit.migration.getGitlabToken() == null ?
                    applicationProperties.gitlab.token : workUnit.migration.getGitlabToken()),
            uri.getAuthority(),
            workUnit.migration.getGitlabGroup(),
            project);
    }

    /**
     * Add remote url to git folder
     * @param workUnit Current work unit
     * @param trunkOnly Only check trunk or not
     */
    private void addRemote(WorkUnit workUnit, boolean trunkOnly) {
        if (workUnit.migration.getTrunk() == null && (trunkOnly || workUnit.migration.getBranches() == null)) {
            try {
                // Set origin
                execCommand(workUnit.directory,
                    buildRemoteCommand(workUnit, null, false),
                    buildRemoteCommand(workUnit, null, true));
            } catch (IOException | InterruptedException | RuntimeException rEx) {
                LOG.debug("Origin already added");
                // Skip
                // TODO : see to refactor, that's pretty ugly
            }
        }
    }
}
