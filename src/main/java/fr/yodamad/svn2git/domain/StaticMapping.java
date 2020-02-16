package fr.yodamad.svn2git.domain;


import javax.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A StaticMapping.
 */
@Entity
@Table(name = "static_mapping")
public class StaticMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "svn_directory")
    private String svnDirectory;

    @Column(name = "regex")
    private String regex;

    @Column(name = "git_directory")
    private String gitDirectory;

    @Column(name = "svn_directory_delete")
    private Boolean svnDirectoryDelete;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSvnDirectory() {
        return svnDirectory;
    }

    public StaticMapping svnDirectory(String svnDirectory) {
        this.svnDirectory = svnDirectory;
        return this;
    }

    public void setSvnDirectory(String svnDirectory) {
        this.svnDirectory = svnDirectory;
    }

    public String getRegex() {
        return regex;
    }

    public StaticMapping regex(String regex) {
        this.regex = regex;
        return this;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getGitDirectory() {
        return gitDirectory;
    }

    public StaticMapping gitDirectory(String gitDirectory) {
        this.gitDirectory = gitDirectory;
        return this;
    }

    public void setGitDirectory(String gitDirectory) {
        this.gitDirectory = gitDirectory;
    }

    public Boolean isSvnDirectoryDelete() {
        return svnDirectoryDelete;
    }

    public StaticMapping svnDirectoryDelete(Boolean svnDirectoryDelete) {
        this.svnDirectoryDelete = svnDirectoryDelete;
        return this;
    }

    public void setSvnDirectoryDelete(Boolean svnDirectoryDelete) {
        this.svnDirectoryDelete = svnDirectoryDelete;
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
        StaticMapping staticMapping = (StaticMapping) o;
        if (staticMapping.getId() == null || getId() == null) {
            return false;
        }
        return Objects.equals(getId(), staticMapping.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "StaticMapping{" +
            "id=" + getId() +
            ", svnDirectory='" + getSvnDirectory() + "'" +
            ", regex='" + getRegex() + "'" +
            ", gitDirectory='" + getGitDirectory() + "'" +
            ", svnDirectoryDelete='" + isSvnDirectoryDelete() + "'" +
            "}";
    }
}
