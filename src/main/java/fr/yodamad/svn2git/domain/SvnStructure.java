package fr.yodamad.svn2git.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * SVN repository structure (only modules)
 */
public class SvnStructure {

    /** SVN structure name. */
    public String name;
    /** Flag for flat repository. */
    public boolean flat = false;
    /** SVN modules. */
    public List<SvnModule> modules = new ArrayList<>();

    public SvnStructure(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (flat) {
            return String.format("Flat structure %s", name);
        }
        return String.format("Structure %s with modules %s", name, modules);
    }

    /**
     * SVN module description
     */
    public static class SvnModule extends SvnStructure {

        /** Used to trace if trunk, branches, tags associated with the module **/
        public Set<String> layoutElements = new LinkedHashSet<String>();
        /** Path in repository. */
        public String path;
        /** Potential submodules. */
        public List<SvnModule> subModules = new ArrayList<>();
        /** Flag for flat module (no trunk, tags or branches) **/
        public Boolean isFlat = false;

        public SvnModule(String name, String path) {
            super(name);
            this.path = String.format("%s/%s", path, name);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name;
        }
    }
}
