package fr.yodamad.svn2git.domain;

import com.fasterxml.jackson.annotation.JsonView;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.web.rest.util.View;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A Migration.
 */
@Entity
@Table(name = "migration")
public class Migration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonView(View.Public.class)
    private Long id;

    @NotNull
    @Column(name = "svn_group", nullable = false)
    @JsonView(View.Public.class)
    private String svnGroup;

    @NotNull
    @Column(name = "svn_project", nullable = false)
    @JsonView(View.Public.class)
    private String svnProject;

    @NotNull
    @Column(name = "jhi_user", nullable = false)
    @JsonView(View.Public.class)
    private String user;

    @Column(name = "jhi_date")
    @JsonView(View.Public.class)
    private LocalDate date;

    @NotNull
    @Column(name = "gitlab_group", nullable = false)
    @JsonView(View.Public.class)
    private String gitlabGroup;

    @NotNull
    @Column(name = "gitlab_project", nullable = false)
    @JsonView(View.Public.class)
    private String gitlabProject;

    @Column(name = "gitlab_project_id")
    @JsonView(View.Public.class)
    private Integer gitlabProjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JsonView(View.Public.class)
    private StatusEnum status;

    @Column(name = "max_file_size")
    @JsonView(View.Public.class)
    private String maxFileSize;

    @Column(name = "forbidden_file_extensions")
    @JsonView(View.Public.class)
    private String forbiddenFileExtensions;

    @Column(name = "gitlab_url")
    @JsonView(View.Public.class)
    private String gitlabUrl;

    @Column(name = "gitlab_token")
    @JsonView(View.Internal.class)
    private String gitlabToken;

    @Column(name = "svn_url")
    @JsonView(View.Public.class)
    private String svnUrl;

    @Column(name = "svn_user")
    @JsonView(View.Public.class)
    private String svnUser;

    @Column(name = "svn_password")
    @JsonView(View.Internal.class)
    private String svnPassword;

    @Column(name = "trunk")
    @JsonView(View.Public.class)
    private String trunk;

    @Column(name = "branches")
    @JsonView(View.Public.class)
    private String branches;

    @Column(name = "tags")
    @JsonView(View.Public.class)
    private String tags;

    @Column(name = "svn_history")
    @JsonView(View.Public.class)
    private String svnHistory;

    @Column(name = "flat")
    @JsonView(View.Public.class)
    private Boolean flat = false;

    @Column(name = "svn_revision")
    @JsonView(View.Public.class)
    private String svnRevision;

    @Column(name = "tags_to_migrate")
    @JsonView(View.Public.class)
    private String tagsToMigrate;

    @Column(name = "branches_to_migrate")
    @JsonView(View.Public.class)
    private String branchesToMigrate;

    @Column(name = "created_timestamp")
    @JsonView(View.Public.class)
    private Instant createdTimestamp;

    @Column(name = "working_directory")
    @JsonView(View.Public.class)
    private String workingDirectory;

    @Column(name = "upload_type")
    @JsonView(View.Public.class)
    private String uploadType;

    @Column(name = "emptydirs")
    @JsonView(View.Public.class)
    private Boolean emptyDirs;

    @OneToMany(mappedBy = "migration")
    @OrderBy("id ASC")
    private Set<MigrationHistory> histories = new HashSet<>();
    @OneToMany(mappedBy = "migration")
    private Set<Mapping> mappings = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSvnGroup() {
        return svnGroup;
    }

    public Migration svnGroup(String svnGroup) {
        this.svnGroup = svnGroup;
        return this;
    }

    public void setSvnGroup(String svnGroup) {
        this.svnGroup = svnGroup;
    }

    public String getSvnProject() {
        return svnProject;
    }

    public Migration svnProject(String svnProject) {
        this.svnProject = svnProject;
        return this;
    }

    public void setSvnProject(String svnProject) {
        this.svnProject = svnProject;
    }

    public String getUser() {
        return user;
    }

    public Migration user(String user) {
        this.user = user;
        return this;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public LocalDate getDate() {
        return date;
    }

    public Migration date(LocalDate date) {
        this.date = date;
        return this;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getGitlabGroup() {
        return gitlabGroup;
    }

    public Migration gitlabGroup(String gitlabGroup) {
        this.gitlabGroup = gitlabGroup;
        return this;
    }

    public void setGitlabGroup(String gitlabGroup) {
        this.gitlabGroup = gitlabGroup;
    }

    public String getGitlabProject() {
        return gitlabProject;
    }

    public Migration gitlabProject(String gitlabProject) {
        this.gitlabProject = gitlabProject;
        return this;
    }

    public void setGitlabProject(String gitlabProject) {
        this.gitlabProject = gitlabProject;
    }

    public Integer getGitlabProjectId() {
        return gitlabProjectId;
    }

    public Migration gitlabProjectId(Integer gitlabProjectId) {
        this.gitlabProjectId = gitlabProjectId;
        return this;
    }

    public void setGitlabProjectId(Integer gitlabProjectId) {
        this.gitlabProjectId = gitlabProjectId;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public Migration status(StatusEnum status) {
        this.status = status;
        return this;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public Migration maxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
        return this;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getForbiddenFileExtensions() {
        return forbiddenFileExtensions;
    }

    public Migration forbiddenFileExtensions(String forbiddenFileExtensions) {
        this.forbiddenFileExtensions = forbiddenFileExtensions;
        return this;
    }

    public void setForbiddenFileExtensions(String forbiddenFileExtensions) {
        this.forbiddenFileExtensions = forbiddenFileExtensions;
    }

    public String getGitlabUrl() {
        return gitlabUrl;
    }

    public Migration gitlabUrl(String gitlabUrl) {
        this.gitlabUrl = gitlabUrl;
        return this;
    }

    public void setGitlabUrl(String gitlabUrl) {
        this.gitlabUrl = gitlabUrl;
    }

    public String getGitlabToken() {
        return gitlabToken;
    }

    public Migration gitlabToken(String gitlabToken) {
        this.gitlabToken = gitlabToken;
        return this;
    }

    public void setGitlabToken(String gitlabToken) {
        this.gitlabToken = gitlabToken;
    }

    public String getSvnUrl() {
        return svnUrl;
    }

    public Migration svnUrl(String svnUrl) {
        this.svnUrl = svnUrl;
        return this;
    }

    public void setSvnUrl(String svnUrl) {
        this.svnUrl = svnUrl;
    }

    public String getSvnUser() {
        return svnUser;
    }

    public Migration svnUser(String svnUser) {
        this.svnUser = svnUser;
        return this;
    }

    public void setSvnUser(String svnUser) {
        this.svnUser = svnUser;
    }

    public String getSvnPassword() {
        return svnPassword;
    }

    public Migration svnPassword(String svnPassword) {
        this.svnPassword = svnPassword;
        return this;
    }

    public void setSvnPassword(String svnPassword) {
        this.svnPassword = svnPassword;
    }

    public String getSvnRevision() {
        return svnRevision;
    }

    public void setSvnRevision(String svnRevision) {
        this.svnRevision = svnRevision;
    }

    public String getTrunk() {
        return trunk;
    }

    public Migration trunk(String trunk) {
        this.trunk = trunk;
        return this;
    }

    public void setTrunk(String trunk) {
        this.trunk = trunk;
    }

    public String getBranches() {
        return branches;
    }

    public Migration branches(String branches) {
        this.branches = branches;
        return this;
    }

    public void setBranches(String branches) {
        this.branches = branches;
    }

    public String getTags() {
        return tags;
    }

    public Migration tags(String tags) {
        this.tags = tags;
        return this;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSvnHistory() {
        return svnHistory;
    }

    public Migration svnHistory(String svnHistory) {
        this.svnHistory = svnHistory;
        return this;
    }

    public void setSvnHistory(String svnHistory) {
        this.svnHistory = svnHistory;
    }

    public String getTagsToMigrate() {
        return tagsToMigrate;
    }

    public Migration tagsToMigrate(String tagsToMigrate) {
        this.tagsToMigrate = tagsToMigrate;
        return this;
    }

    public void setTagsToMigrate(String tagsToMigrate) {
        this.tagsToMigrate = tagsToMigrate;
    }

    public String getBranchesToMigrate() {
        return branchesToMigrate;
    }

    public Migration branchesToMigrate(String branchesToMigrate) {
        this.branchesToMigrate = branchesToMigrate;
        return this;
    }

    public void setBranchesToMigrate(String branchesToMigrate) {
        this.branchesToMigrate = branchesToMigrate;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Migration createdTimestamp(Instant createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
        return this;
    }

    public void setCreatedTimestamp(Instant createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Migration workingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public Set<MigrationHistory> getHistories() {
        return histories;
    }

    public Migration histories(Set<MigrationHistory> migrationHistories) {
        this.histories = migrationHistories;
        return this;
    }

    public Migration addHistory(MigrationHistory migrationHistory) {
        this.histories.add(migrationHistory);
        migrationHistory.setMigration(this);
        return this;
    }

    public Migration removeHistory(MigrationHistory migrationHistory) {
        this.histories.remove(migrationHistory);
        migrationHistory.setMigration(null);
        return this;
    }

    public void setHistories(Set<MigrationHistory> migrationHistories) {
        this.histories = migrationHistories;
    }

    public Set<Mapping> getMappings() {
        return mappings;
    }

    public Migration mappings(Set<Mapping> mappings) {
        this.mappings = mappings;
        return this;
    }

    public Migration addMappings(Mapping mapping) {
        this.mappings.add(mapping);
        mapping.setMigration(this.getId());
        return this;
    }

    public Migration removeMappings(Mapping mapping) {
        this.mappings.remove(mapping);
        mapping.setMigration(null);
        return this;
    }

    public void setMappings(Set<Mapping> mappings) {
        this.mappings = mappings;
    }

    public Boolean getFlat() {
        return flat;
    }

    public void setFlat(Boolean flat) {
        this.flat = flat;
    }

    public String getUploadType() { return uploadType; }

    public void setUploadType(String uploadType) { this.uploadType = uploadType; }

    public Boolean getEmptyDirs() { return emptyDirs; }

    public void setEmptyDirs(Boolean emptyDirs) { this.emptyDirs = emptyDirs; }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Migration migration = (Migration) o;
        if (migration.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), migration.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Migration{" +
            "id=" + getId() +
            ", svnGroup='" + getSvnGroup() + "'" +
            ", svnProject='" + getSvnProject() + "'" +
            ", user='" + getUser() + "'" +
            ", date='" + getDate() + "'" +
            ", gitlabGroup='" + getGitlabGroup() + "'" +
            ", gitlabProject='" + getGitlabProject() + "'" +
            ", status='" + getStatus() + "'" +
            ", maxFileSize='" + getMaxFileSize() + "'" +
            ", forbiddenFileExtensions='" + getForbiddenFileExtensions() + "'" +
            ", gitlabUrl='" + getGitlabUrl() + "'" +
            ", svnUrl='" + getSvnUrl() + "'" +
            ", svnUser='" + getSvnUser() + "'" +
            ", trunk='" + getTrunk() + "'" +
            ", branches='" + getBranches() + "'" +
            ", tags='" + getTags() + "'" +
            ", flat='" + getFlat() + "'" +
            ", svnHistory='" + getSvnHistory() + "'" +
            ", tagsToMigrate='" + getTagsToMigrate() + "'" +
            ", branchesToMigrate='" + getBranchesToMigrate() + "'" +
            ", createdTimestamp='" + getCreatedTimestamp() + "'" +
            ", workingDirectory='" + getWorkingDirectory() + "'" +
            "}";
    }
}
