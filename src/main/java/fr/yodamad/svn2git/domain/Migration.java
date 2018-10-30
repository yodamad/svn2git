package fr.yodamad.svn2git.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;

import fr.yodamad.svn2git.domain.enumeration.StatusEnum;

/**
 * A Migration.
 */
@Entity
@Table(name = "migration")
public class Migration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "svn_group", nullable = false)
    private String svnGroup;

    @NotNull
    @Column(name = "svn_project", nullable = false)
    private String svnProject;

    @NotNull
    @Column(name = "jhi_user", nullable = false)
    private String user;

    @Column(name = "jhi_date")
    private LocalDate date;

    @NotNull
    @Column(name = "gitlab_group", nullable = false)
    private String gitlabGroup;

    @NotNull
    @Column(name = "gitlab_project", nullable = false)
    private String gitlabProject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusEnum status;

    @Column(name = "max_file_size")
    private String maxFileSize;

    @Column(name = "forbidden_file_extensions")
    private String forbiddenFileExtensions;

    @OneToMany(mappedBy = "migration")
    private Set<MigrationHistory> histories = new HashSet<>();
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
            "}";
    }
}
