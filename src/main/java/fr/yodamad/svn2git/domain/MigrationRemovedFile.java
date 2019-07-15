package fr.yodamad.svn2git.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.yodamad.svn2git.domain.enumeration.Reason;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * A MigrationRemovedFile.
 */
@Entity
@Table(name = "migration_removed_file")
public class MigrationRemovedFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "svn_location", nullable = false)
    private String svnLocation;

    @Column(name = "path")
    private String path;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private Reason reason;

    @Column(name = "file_size")
    private Long fileSize;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties("")
    @NotNull
    private Migration migration;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSvnLocation() {
        return svnLocation;
    }

    public MigrationRemovedFile svnLocation(String svnLocation) {
        this.svnLocation = svnLocation;
        return this;
    }

    public void setSvnLocation(String svnLocation) {
        this.svnLocation = svnLocation;
    }

    public String getPath() {
        return path;
    }

    public MigrationRemovedFile path(String path) {
        this.path = path;
        return this;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Reason getReason() {
        return reason;
    }

    public MigrationRemovedFile reason(Reason reason) {
        this.reason = reason;
        return this;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public MigrationRemovedFile fileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Migration getMigration() {
        return migration;
    }

    public MigrationRemovedFile migration(Migration migration) {
        this.migration = migration;
        return this;
    }

    public void setMigration(Migration migration) {
        this.migration = migration;
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
        MigrationRemovedFile migrationRemovedFile = (MigrationRemovedFile) o;
        if (migrationRemovedFile.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), migrationRemovedFile.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "MigrationRemovedFile{" +
            "id=" + getId() +
            ", svnLocation='" + getSvnLocation() + "'" +
            ", path='" + getPath() + "'" +
            ", reason='" + getReason() + "'" +
            ", fileSize=" + getFileSize() +
            "}";
    }
}
