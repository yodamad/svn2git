package fr.yodamad.svn2git.service;

import fr.yodamad.svn2git.config.ApplicationProperties;
import fr.yodamad.svn2git.config.Constants;
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
import org.springframework.beans.factory.annotation.Lookup;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.yodamad.svn2git.service.util.MigrationConstants.GIT_PUSH;
import static fr.yodamad.svn2git.service.util.MigrationConstants.MASTER;
import static fr.yodamad.svn2git.service.util.MigrationConstants.ORIGIN_TAGS;
import static fr.yodamad.svn2git.service.util.MigrationConstants.STARS;
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
    private static final String FAILED_TO_PUSH_BRANCH = "Failed to push branch";
    private static ApplicationProperties applicationProperties;
    // Manager & repository
    private final HistoryManager historyMgr;
    private final MappingRepository mappingRepository;

    public GitManager(final HistoryManager historyManager,
                      final MappingRepository mappingRepository,
                      final ApplicationProperties applicationProperties) {
        this.historyMgr = historyManager;
        this.mappingRepository = mappingRepository;
        GitManager.applicationProperties = applicationProperties;
    }

    /**
     * Assure passwords get escaped correctly.
     * printf on linux allows us to escape linux characters at runtime
     *
     * @param secret
     * @return
     */
    private static String getEscapedSecret(String secret) {

        if (StringUtils.isNotBlank(secret)) {

            if (Shell.isWindows || applicationProperties.flags.isAlpine()) {
                // For development environment
                return format("echo %s|", secret);

            } else {
                // first escape any double quotes in the password
                String escapedQuotesSecret = secret.replace("\"", "\\\"");
                // now generate printf "%q" command that will escape any special linux characters at runtime
                // TODO : Check if we need to remove the space before the PIPE so that we are sure using passed password
                return "printf \"%q\" \"" + escapedQuotesSecret + "\" |";
            }

        } else {

            return "";
        }

    }

    /**
     * Generate IgnoreRefs strings used as a parameter for git svn
     *
     * @param branchesToMigrate
     * @param tagsToMigrate
     * @return
     */
    public static String generateIgnoreRefs(String branchesToMigrate, String tagsToMigrate) {

        // refs/remote/origin/branchname
        List<String> branchesToMigrateList = new ArrayList<>();
        if (StringUtils.isNotBlank(branchesToMigrate)) {
            String[] branchesToMigrateArray = branchesToMigrate.split(",");

            if (branchesToMigrateArray != null) {
                branchesToMigrateList = Arrays.asList(branchesToMigrateArray);
            }
        }


        List<String> tagsToMigrateList = new ArrayList<>();
        if (StringUtils.isNotBlank(tagsToMigrate)) {
            String[] tagsToMigrateArray = tagsToMigrate.split(",");

            if (tagsToMigrateArray != null) {
                tagsToMigrateList = Arrays.asList(tagsToMigrateArray);
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!branchesToMigrateList.isEmpty() || !tagsToMigrateList.isEmpty()) {
            // examples
            // BranchANDTagProvided : --ignore-refs="^refs/remotes/origin/(?!branch1|tags/tag1).*$"
            // BranchONLYProvided   : --ignore-refs="^refs/remotes/origin/(?!branch1|tags/).*$"
            // TagONLYProvided      : --ignore-refs="^refs/remotes/origin/tags/(?!tag1).*$"

            if (branchesToMigrateList.isEmpty()) {
                // Keep all branches AND named tags
                sb.append("--ignore-refs=\"^refs/remotes/origin/tags/(?!");
            } else {
                // Keep named branches AND named tags
                // OR
                // Keep all tags AND named branches
                sb.append("--ignore-refs=\"^refs/remotes/origin/(?!");
            }

            if (!branchesToMigrateList.isEmpty()) {

                Iterator<String> branchesIter = branchesToMigrateList.iterator();
                while (branchesIter.hasNext()) {
                    String branch = branchesIter.next();
                    sb.append(branch.trim().replace(".", "\\."));
                    sb.append("|");
                }
            }

            if (!tagsToMigrateList.isEmpty()) {

                Iterator<String> tagsIter = tagsToMigrateList.iterator();
                while (tagsIter.hasNext()) {
                    String tag = tagsIter.next();
                    if (!branchesToMigrateList.isEmpty()) {
                        sb.append("tags/");
                    }
                    sb.append(tag.trim().replace(".", "\\."));

                    if (tagsIter.hasNext()) {
                        sb.append("|");
                    }
                }
            } else {
                sb.append("tags/");
            }

            sb.append(").*$\"");

            return sb.toString();
        }

        return "";
    }

    private static String generateIgnorePaths(String trunk, String tags, String branches, String svnProject, List<String> deleteSvnFolderList) {

        // generate something like this
        if (deleteSvnFolderList == null || deleteSvnFolderList.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--ignore-paths=");

        // Whole expression surrounded by quotation marks
        sb.append("\"");

        // ********* START Dynamic Block SVN Paths ignored according to whether trunk, branches or tags selected
        sb.append("^(");

        if (trunk != null) {
            sb.append(svnProject.replaceFirst("/", "/?"))
                .append("/trunk/");

            if (tags != null || branches != null) {
                sb.append("|");
            }
        }

        if (tags != null) {
            sb.append(svnProject.replaceFirst("/", "/?"))
                .append("/tags/[^/]+/");

            if (branches != null) {
                sb.append("|");
            }
        }

        // branches regex is just like trunk in fact
        if (branches != null) {
            sb.append(svnProject.replaceFirst("/", "/?"))
                .append("/branches/");
        }

        sb.append(")(");

        Iterator it = deleteSvnFolderList.iterator();

        while (it.hasNext()) {

            sb.append(it.next())
                .append("/");

            if (it.hasNext()) {
                sb.append("|");
            }
        }

        sb.append(").*");

        // END Quotation marks
        sb.append("\"");

        // NOTE : Apparently can't select to ignore-path of trunk and keep same path of tag
        // (e.g. keep 05_impl/1_bin of tag only). Likely because tag is based on trunk (which has to exist)...

        return sb.toString();

    }

    /**
     * List SVN branches & tags cloned
     *
     * @param directory working directory
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    static List<String> listRemotes(String directory) throws InterruptedException, IOException {
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
     * List only branches
     *
     * @param remotes Remote list
     * @return list containing only branches
     */
    static List<String> listBranchesOnly(List<String> remotes) {
        return remotes.stream()
            .map(String::trim)
            // Remove tags
            .filter(b -> !b.startsWith(ORIGIN_TAGS))
            // Remove master/trunk
            .filter(b -> !b.contains(MASTER))
            .filter(b -> !b.contains("trunk"))
            .filter(b -> !b.contains("@"))
            .collect(Collectors.toList());
    }

    /**
     * List only tags
     *
     * @param remotes Remote list
     * @return list containing only tags
     */
    static List<String> listTagsOnly(List<String> remotes) {
        return remotes.stream()
            .map(String::trim)
            // Only tags
            .filter(b -> b.startsWith(ORIGIN_TAGS))
            // Remove temp tags
            .filter(b -> !b.contains("@"))
            .collect(Collectors.toList());
    }

    static private boolean isFileInFolder(String dirPath) {

        boolean isFileInFolder = false;

        File f = new File(dirPath);
        File[] files = f.listFiles();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {

                File file = files[i];

                // Only check subfolders if haven't found a file yet
                if (file.isDirectory()) {

                    if (!file.getName().equalsIgnoreCase(".git")) {
                        isFileInFolder = isFileInFolder(file.getAbsolutePath());
                        if (isFileInFolder) {
                            return true;
                        }
                    } else {
                        LOG.info("Skipping check for files in .git folder");
                    }
                } else {
                    LOG.info("Found at least one file in this folder: " + file.getAbsolutePath());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get freshinstance of GitlabAdmin object when called.
     *
     * @return GitlabAdmin
     */
    @Lookup
    public GitlabAdmin getGitlabAdminPrototype() {
        return null;
    }

    /**
     * Create project in GitLab
     *
     * @param migration
     * @param workUnit
     * @throws GitLabApiException
     */
    public void createGitlabProject(Migration migration, WorkUnit workUnit) throws GitLabApiException {

        MigrationHistory history = historyMgr.startStep(migration, StepEnum.GITLAB_PROJECT_CREATION, migration.getGitlabUrl() + migration.getGitlabGroup());

        GitlabAdmin gitlabAdmin = getGitlabAdminPrototype();

        // If gitlabInfo.token is empty assure using values found in application.yml.
        // i.e. those in default GitlabAdmin object
        if (StringUtils.isEmpty(migration.getGitlabToken())) {
            LOG.info("Already using default url and token");
        } else {
            // If gitlabInfo.token has a value we overide as appropriate
            if (!gitlabAdmin.api().getGitLabServerUrl().equalsIgnoreCase(migration.getGitlabUrl()) ||
                !gitlabAdmin.api().getAuthToken().equalsIgnoreCase(migration.getGitlabToken())) {

                LOG.info("Overiding gitlab url and token");
                gitlabAdmin = new GitlabAdmin(GitManager.applicationProperties);
                gitlabAdmin.setGitLabApi(new GitLabApi(migration.getGitlabUrl(), migration.getGitlabToken()));
            }
        }

        try {
            Group group = gitlabAdmin.groupApi().getGroup(migration.getGitlabGroup());

            // If no svn project specified, use svn group instead
            if (StringUtils.isEmpty(migration.getSvnProject())) {
                gitlabAdmin.projectApi().createProject(group.getId(), migration.getSvnGroup());
                historyMgr.endStep(history, StatusEnum.DONE, null);
                // return migration.getSvnGroup();
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
                    // return structure[structure.length - 1];
                } else {
                    throw new GitLabApiException("Please remove the destination project '" + group.getName() + "/" + structure[structure.length - 1]);
                }
            }
        } catch (GitLabApiException exc) {
            String message = exc.getMessage().replace(applicationProperties.gitlab.token, STARS);
            historyMgr.endStep(history, StatusEnum.FAILED, message);
            throw exc;
        }

    }

    /**
     * Git svn clone command to copy svn as git repository
     *
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

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.SVN_CHECKOUT,
            (workUnit.commandManager.isFirstAttemptMigration() ? "" : Constants.REEXECUTION_SKIPPING) + safeCommand);
        // Only Clone if first attempt at migration
        if (workUnit.commandManager.isFirstAttemptMigration()) {
            execCommand(workUnit.commandManager, workUnit.root, cloneCommand, safeCommand);
        }
        historyMgr.endStep(history, StatusEnum.DONE, null);

    }

    /**
     * Init command with or without password in clear
     *
     * @param workUnit Current workUnit
     * @param username Username to use
     * @param secret   Escaped password
     * @return
     */
    private String initCommand(WorkUnit workUnit, String username, String secret) {

        // Get list of svnDirectoryDelete
        List<String> svnDirectoryDeleteList = getSvnDirectoryDeleteList(workUnit.migration.getId());
        // Initialise ignorePaths string that will be passed to git svn clone
        String ignorePaths = generateIgnorePaths(workUnit.migration.getTrunk(), workUnit.migration.getTags(), workUnit.migration.getBranches(), workUnit.migration.getSvnProject(), svnDirectoryDeleteList);

        // regex with negative look forward allows us to choose the branch and tag names to keep
        String ignoreRefs = generateIgnoreRefs(workUnit.migration.getBranchesToMigrate(), workUnit.migration.getTagsToMigrate());


        // String sCommand = format("%s git svn clone --no-minimize-url --include-paths=.*bosCapalogImport.* %s %s %s %s %s %s %s %s%s",
        String sCommand = format("%s git svn clone %s %s %s %s %s %s %s %s%s",
            StringUtils.isEmpty(secret) ? "" : isWindows ? format("echo(%s|", secret) : format("echo %s |", secret),
            StringUtils.isEmpty(username) ? "" : format("--username %s", username),
            workUnit.migration.getTrunk() == null ? "" : format("--trunk=%s/trunk", workUnit.migration.getSvnProject()),
            workUnit.migration.getBranches() == null ? "" : format("--branches=%s/branches", workUnit.migration.getSvnProject()),
            workUnit.migration.getTags() == null ? "" : format("--tags=%s/tags", workUnit.migration.getSvnProject()),
            StringUtils.isEmpty(ignorePaths) ? "" : ignorePaths,
            StringUtils.isEmpty(ignoreRefs) ? "" : ignoreRefs,
            applicationProperties.getFlags().isGitSvnClonePreserveEmptyDirsOption() ? "--preserve-empty-dirs" : "",
            workUnit.migration.getSvnUrl().endsWith("/") ? workUnit.migration.getSvnUrl() : format("%s/", workUnit.migration.getSvnUrl()),
            workUnit.migration.getSvnGroup());

        // replace any multiple whitespaces and return
        return sCommand.replaceAll("\\s{2,}", " ").trim();
    }

    /**
     * return list of svnDirectories to delete from a Set of Mappings
     *
     * @param migrationId
     * @return
     */
    private List<String> getSvnDirectoryDeleteList(Long migrationId) {

        List<Mapping> mappings = mappingRepository.findByMigrationAndSvnDirectoryDelete(migrationId, true);

        List<String> svnDirectoryDeleteList = new ArrayList<>();

        Iterator<Mapping> it = mappings.iterator();
        while (it.hasNext()) {
            Mapping mp = it.next();
            if (mp.isSvnDirectoryDelete()) {
                svnDirectoryDeleteList.add(mp.getSvnDirectory());
            }
        }

        return svnDirectoryDeleteList;
    }

    /**
     * Apply mappings configured
     *
     * @param workUnit
     * @param branch   Branch to process
     */
    public boolean applyMapping(WorkUnit workUnit, String branch) {
        // Get only the mappings (i.e. where svnDirectoryDelete is false)
        List<Mapping> mappings = mappingRepository.findByMigrationAndSvnDirectoryDelete(workUnit.migration.getId(), false);
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
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                gitCommand = format("git commit -m \"Apply mappings on %s\"", branch);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                // git push
                gitCommand = format("%s --set-upstream origin %s", GIT_PUSH, branch.replace("origin/", ""));
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

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
     *
     * @param workUnit
     * @param mapping  Mapping to apply
     * @param branch   Current branch
     */
    private StatusEnum mvDirectory(WorkUnit workUnit, Mapping mapping, String branch) {
        MigrationHistory history;
        String msg = format("git mv %s %s \"%s\" \"%s\" on %s", (applicationProperties.getFlags().isGitMvFOption() ? "-f" : ""), (applicationProperties.getFlags().isGitMvKOption() ? "-k" : ""), mapping.getSvnDirectory(), mapping.getGitDirectory(), branch);

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
     *
     * @param workUnit Current work unit
     * @param mapping  Mapping to apply
     * @param branch   Current branch
     */
    private StatusEnum mvRegex(WorkUnit workUnit, Mapping mapping, String branch) {
        String msg = format("git mv %s %s \"%s\" \"%s\" based on regex %s on %s", (applicationProperties.getFlags().isGitMvFOption() ? "-f" : ""), (applicationProperties.getFlags().isGitMvKOption() ? "-k" : ""), mapping.getSvnDirectory(), mapping.getGitDirectory(), mapping.getRegex(), branch);
        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, msg);

        String regex = mapping.getRegex();
        if (mapping.getRegex().startsWith("*")) {
            regex = '.' + mapping.getRegex();
        }

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
                        return execCommand(workUnit.commandManager, workUnit.directory, format("git mv %s %s \"%s\" \"%s\"", (applicationProperties.getFlags().isGitMvFOption() ? "-f" : ""), (applicationProperties.getFlags().isGitMvKOption() ? "-k" : ""), el, Paths.get(mapping.getGitDirectory(), el).toString()));
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
     *
     * @param workUnit
     * @param svnDir   Origin SVN element
     * @param gitDir   Target Git element
     * @param branch   Current branch
     */
    private StatusEnum mv(WorkUnit workUnit, String svnDir, String gitDir, String branch, boolean traceStep) {

        try {
            long gitMvPauseMilliSeconds = applicationProperties.getGitlab().getGitMvPauseMilliSeconds();
            if (gitMvPauseMilliSeconds > 0) {
                LOG.info(format("Waiting %d MilliSeconds between git mv operations", gitMvPauseMilliSeconds));
                Thread.sleep(gitMvPauseMilliSeconds);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        MigrationHistory history = null;
        try {
            String historyCommand = format("git mv %s %s \"%s\" \"%s\" on %s", (applicationProperties.getFlags().isGitMvFOption() ? "-f" : ""), (applicationProperties.getFlags().isGitMvKOption() ? "-k" : ""), svnDir, gitDir, branch);
            String gitCommand = format("git mv %s %s \"%s\" \"%s\"", (applicationProperties.getFlags().isGitMvFOption() ? "-f" : ""), (applicationProperties.getFlags().isGitMvKOption() ? "-k" : ""), svnDir, gitDir);
            if (traceStep) history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_MV, historyCommand);
            // git mv
            int exitCode = execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

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
     *
     * @param workUnit
     * @param remotes
     */
    void manageBranches(WorkUnit workUnit, List<String> remotes) {
        listBranchesOnly(remotes).forEach(b -> {
                final boolean warn = pushBranch(workUnit, b);
                workUnit.warnings.set(workUnit.warnings.get() || warn);

                if (applicationProperties.gitlab.gitPushPauseMilliSeconds > 0) {
                    try {
                        LOG.info(String.format("Waiting gitPushPauseMilliSeconds:%s",
                            applicationProperties.gitlab.gitPushPauseMilliSeconds));
                        Thread.sleep(applicationProperties.gitlab.gitPushPauseMilliSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        );
    }

    /**
     * Push a branch
     *
     * @param workUnit
     * @param branch   Branch to migrate
     */
    boolean pushBranch(WorkUnit workUnit, String branch) throws RuntimeException {
        String branchName = branch.replaceFirst("refs/remotes/origin/", "");
        branchName = branchName.replaceFirst("origin/", "");
        LOG.debug(format("Branch %s", branchName));

        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, branchName);
        String gitCommand = format("git checkout -b %s %s", branchName, branch);
        try {
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
        } catch (IOException | InterruptedException iEx) {
            LOG.error(FAILED_TO_PUSH_BRANCH, iEx);
            historyMgr.endStep(history, StatusEnum.FAILED, iEx.getMessage());
            return false;
        }

        if (workUnit.migration.getSvnHistory().equals("all")) {
            try {
                addRemote(workUnit, true);

                gitCommand = format("%s --set-upstream origin %s", GIT_PUSH, branchName);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
                historyMgr.endStep(history, StatusEnum.DONE, null);
            } catch (IOException | InterruptedException iEx) {
                LOG.error(FAILED_TO_PUSH_BRANCH, iEx);
                historyMgr.endStep(history, StatusEnum.FAILED, iEx.getMessage());
                return false;
            }
        } else {
            removeHistory(workUnit, branchName, false, history);
        }
        return applyMapping(workUnit, branch);
    }

    /**
     * Manage tags extracted from SVN
     *
     * @param workUnit
     * @param remotes
     */
    void manageTags(WorkUnit workUnit, List<String> remotes) {
        listTagsOnly(remotes).forEach(t -> {
                final boolean warn = pushTag(workUnit, t);
                workUnit.warnings.set(workUnit.warnings.get() || warn);

                if (applicationProperties.gitlab.gitPushPauseMilliSeconds > 0) {
                    try {
                        LOG.info(String.format("Waiting gitPushPauseMilliSeconds:%s",
                            applicationProperties.gitlab.gitPushPauseMilliSeconds));
                        Thread.sleep(applicationProperties.gitlab.gitPushPauseMilliSeconds);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        );
    }

    /**
     * Push a tag
     *
     * @param workUnit Current work unit
     * @param tag      Tag to migrate
     */
    private boolean pushTag(WorkUnit workUnit, String tag) {
        MigrationHistory history = historyMgr.startStep(workUnit.migration, StepEnum.GIT_PUSH, tag);
        try {

            // derive local tagName from remote tag name
            String tagName = tag.replaceFirst(ORIGIN_TAGS, "");
            LOG.debug(format("Tag %s", tagName));

            // determine noHistory flag i.e was all selected or not
            boolean noHistory = !workUnit.migration.getSvnHistory().equals("all");

            // checkout a new branch using local tagName and remote tag name
            String gitCommand = format("git checkout -b tmp_tag %s", tag);
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

            // If this tag does not contain any files we will ignore it and add warning to logs.
            if (!isFileInFolder(workUnit.directory)) {

                // Switch over to master
                gitCommand = "git checkout master";
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                // Now we can delete the branch tmp_tag
                gitCommand = "git branch -D tmp_tag";
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                historyMgr.endStep(history, StatusEnum.IGNORED, "Ignoring Tag: " + tag + " : Because there are no files to commit.");

            } else {

                // creates a temporary orphan branch and renames it to tmp_tag
                if (noHistory) {
                    removeHistory(workUnit, "tmp_tag", true, history);
                }

                // Checkout master.
                gitCommand = "git checkout master";
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                // create tag from tmp_tag branch.
                gitCommand = format("git tag %s tmp_tag", tagName);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                // add remote to master
                addRemote(workUnit, false);

                // push the tag to remote
                // crashes if branch with same name so prefixing with refs/tags/
                gitCommand = format("git push -u origin refs/tags/%s", tagName);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                // delete the tmp_tag branch now that the tag has been created.
                gitCommand = "git branch -D tmp_tag";
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                historyMgr.endStep(history, StatusEnum.DONE, null);

            }

        } catch (IOException | InterruptedException gitEx) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
        }
        return false;
    }

    /**
     * Remove commit history on a given branch
     *
     * @param workUnit Current work unit
     * @param branch   Branch to work on
     * @param isTag    Flag to check if working on a tag
     * @param history  Current history instance
     */
    public void removeHistory(WorkUnit workUnit, String branch, boolean isTag, MigrationHistory history) {
        try {
            LOG.debug(format("Remove history on %s", branch));

            // Create new orphan branch and switch to it. The first commit made on this new branch
            // will have no parents and it will be the root of a new history totally disconnected from all the
            // other branches and commits
            String gitCommand = format("git checkout --orphan TEMP_BRANCH_%s", branch);
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

            // Stage All (new, modified, deleted) files. Equivalent to git add . (in Git Version 2.x)
            gitCommand = "git add -A";
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

            try {
                // Create a new commit. Runs git add on any file that is 'tracked' and provide a message
                // for the commit
                gitCommand = format("git commit -am \"Reset history on %s\"", branch);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
            } catch (RuntimeException ex) {
                // Ignored failed step
            }

            try {
                // Delete (with force) the passed in branch name
                gitCommand = format("git branch -D %s", branch);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);
            } catch (RuntimeException ex) {
                if (ex.getMessage().equalsIgnoreCase("1")) {
                    // Ignored failed step
                }
            }

            // move/rename a branch and the corresponing reflog
            // (i.e. rename the orphan branch - without history - to the passed in branch name)
            // Note : This fails with exit code 128 (git branch -m tmp_tag) when only folders in the subversion tag.
            // git commit -am above fails because no files
            gitCommand = format("git branch -m %s", branch);
            execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

            // i.e. if it is a branch
            if (!isTag) {

                // create the remote
                addRemote(workUnit, true);

                // push to remote
                gitCommand = format("git push -f origin %s", branch);
                execCommand(workUnit.commandManager, workUnit.directory, gitCommand);

                historyMgr.endStep(history, StatusEnum.DONE, format("Push %s with no history", branch));
            }

        } catch (IOException | InterruptedException gitEx) {
            LOG.error(FAILED_TO_PUSH_BRANCH, gitEx);
            historyMgr.endStep(history, StatusEnum.FAILED, gitEx.getMessage());
        }
    }


    /**
     * Build command to add remote
     *
     * @param workUnit Current work unit
     * @param project  Current project
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
                applicationProperties.gitlab.account : workUnit.migration.getUser(),
            safeMode ? STARS :
                (workUnit.migration.getGitlabToken() == null ?
                    applicationProperties.gitlab.token : workUnit.migration.getGitlabToken()),
            uri.getAuthority(),
            workUnit.migration.getGitlabGroup(),
            project);
    }

    /**
     * Add remote url to git folder
     *
     * @param workUnit  Current work unit
     * @param trunkOnly Only check trunk or not
     */
    private void addRemote(WorkUnit workUnit, boolean trunkOnly) {
        if (workUnit.migration.getTrunk() == null && (trunkOnly || workUnit.migration.getBranches() == null)) {
            try {
                // Set origin
                execCommand(workUnit.commandManager, workUnit.directory,
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
