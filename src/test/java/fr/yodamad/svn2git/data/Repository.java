package fr.yodamad.svn2git.data;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

public class Repository {

    public String name;
    public String namespace;

    public List<String> keep = new ArrayList<>();
    public List<String> remove = new ArrayList<>();

    public List<String> allFiles() { return concat(keep.stream(), remove.stream()).collect(toList()); }

    public static List<String> ALL_MODULES = asList(Modules.MODULE_1, Modules.MODULE_2, Modules.MODULE_3, Modules.SUBMODULE_1, Modules.SUBMODULE_2);

    public Repository() {}
    public Repository(String nmsp, String name) {
        this.name = name;
        this.namespace = nmsp;
    }

    public static Repository simple() {
        Repository repository = new Repository();
        repository.name = "simple";
        repository.namespace = "simple";
        repository.keep.add(Files.REVISION);
        return repository;
    }

    public static Repository complex() {
        Repository repository = new Repository();
        repository.name = "module1/submodule1";
        repository.namespace = "complex";
        repository.keep.add(Files.REVISION);
        return repository;
    }

    public static Repository flat() {
        Repository repository = new Repository();
        repository.name = "module1";
        repository.namespace = "flat_complex";
        repository.keep.add(Files.REVISION);
        return repository;
    }

    public static Repository weird() {
        Repository repository = new Repository();
        repository.name = "weird";
        repository.namespace = "weird";
        repository.keep.add(Files.REVISION);
        return repository;
    }

    public class Files {
        public static final String REVISION = "revision.txt";
        public static final String FILE_BIN = "file.bin";
        public static final String DEEP_FILE = "deep.file";
        public static final String FLAT_FILE = "flat.file";
        public static final String ROOT_ANOTHER_BIN = "another.bin";
        public static final String ANOTHER_BIN = Dirs.FOLDER + ROOT_ANOTHER_BIN;
        public static final String EMPTY_DIR = "folder/empty";
        public static final String MAPPED_ANOTHER_BIN = Dirs.DIRECTORY + "another.bin";
        public static final String JAVA = Dirs.FOLDER + "App.java";
        public static final String MAPPED_JAVA = Dirs.DIRECTORY + "App.java";
        public static final String DEEP = Dirs.FOLDER + Dirs.SUBFOLDER + DEEP_FILE;
        public static final String MAPPED_DEEP = Dirs.DIRECTORY + Dirs.SUBFOLDER + DEEP_FILE;
    }

    public class Dirs {
        public static final String DIRECTORY = "directory/";
        public static final String FOLDER = "folder/";
        public static final String SUBFOLDER = "subfolder/";
        public static final String EMPTY = "empty/";
    }

    public class Branches {
        public static final String MASTER = "master";
        public static final String DEV = "dev";
        public static final String FEATURE = "feature_1";
    }

    public class Tags {
        public static final String V1_0 = "v1.0";
        public static final String V1_1 = "v1.1";
    }

    public class Modules {
        public static final String MODULE_1 = "module1";
        public static final String MODULE_2 = "module2";
        public static final String MODULE_3 = "module3";
        public static final String SUBMODULE_1 = MODULE_1 + "/submodule1";
        public static final String SUBMODULE_2 = MODULE_1 + "/submodule2";
    }
}
