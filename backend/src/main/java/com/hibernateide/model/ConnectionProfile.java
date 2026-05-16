package com.hibernateide.model;

import java.util.ArrayList;
import java.util.List;

public class ConnectionProfile {
    private String name;
    private String jdbcUrl;
    private String username;
    private String password;
    private String dialect;
    private String driverClass;
    private List<String> hbmPaths    = new ArrayList<>();
    private List<String> classesPaths = new ArrayList<>();

    public String getName()                      { return name; }
    public void setName(String v)                { name = v; }
    public String getJdbcUrl()                   { return jdbcUrl; }
    public void setJdbcUrl(String v)             { jdbcUrl = v; }
    public String getUsername()                  { return username; }
    public void setUsername(String v)            { username = v; }
    public String getPassword()                  { return password; }
    public void setPassword(String v)            { password = v; }
    public String getDialect()                   { return dialect; }
    public void setDialect(String v)             { dialect = v; }
    public String getDriverClass()               { return driverClass; }
    public void setDriverClass(String v)         { driverClass = v; }
    public List<String> getHbmPaths()            { return hbmPaths; }
    public void setHbmPaths(List<String> v)      { hbmPaths = v != null ? v : new ArrayList<>(); }
    public List<String> getClassesPaths()        { return classesPaths; }
    public void setClassesPaths(List<String> v)  { classesPaths = v != null ? v : new ArrayList<>(); }
}
