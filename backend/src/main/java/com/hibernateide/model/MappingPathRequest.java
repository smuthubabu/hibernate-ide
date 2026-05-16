package com.hibernateide.model;

import java.util.List;

public class MappingPathRequest {
    private List<String> hbmPaths;      // files or directories containing .hbm.xml files
    private List<String> classesPaths;  // JAR files or compiled classes directories

    public List<String> getHbmPaths()           { return hbmPaths; }
    public void setHbmPaths(List<String> v)     { hbmPaths = v; }
    public List<String> getClassesPaths()       { return classesPaths; }
    public void setClassesPaths(List<String> v) { classesPaths = v; }
}
