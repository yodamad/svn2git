package fr.yodamad.svn2git.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.yodamad.svn2git.domain.enumeration.StatusEnum;
import fr.yodamad.svn2git.domain.enumeration.StepEnum;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A MigrationHistory.
 */
@Entity
@Table(name = "migration_history")
public class MigrationHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "step")
    private StepEnum step;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusEnum status;

    @Column(name = "jhi_date")
    private Instant date;

    @Column(name = "data", columnDefinition = "text")
    private String data;

    @ManyToOne
    @JsonIgnoreProperties("")
    private Migration migration;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StepEnum getStep() {
        return step;
    }

    public MigrationHistory step(StepEnum step) {
        this.step = step;
        return this;
    }

    public void setStep(StepEnum step) {
        this.step = step;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public MigrationHistory status(StatusEnum status) {
        this.status = status;
        return this;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public Instant getDate() {
        return date;
    }

    public MigrationHistory date(Instant date) {
        this.date = date;
        return this;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public String getData() {
        return data;
    }

    public MigrationHistory data(String data) {
        this.data = data;
        return this;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Migration getMigration() {
        return migration;
    }

    public MigrationHistory migration(Migration migration) {
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
        MigrationHistory migrationHistory = (MigrationHistory) o;
        if (migrationHistory.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), migrationHistory.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "MigrationHistory{" +
            "id=" + getId() +
            ", step='" + getStep() + "'" +
            ", status='" + getStatus() + "'" +
            ", date='" + getDate() + "'" +
            ", data='" + getData() + "'" +
            "}";
    }
}
