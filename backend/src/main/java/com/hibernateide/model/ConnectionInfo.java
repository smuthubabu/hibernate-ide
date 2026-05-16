package com.hibernateide.model;

public class ConnectionInfo {
    private String id;
    private String name;
    private String jdbcUrl;
    private String username;
    private String dialect;
    private String status;
    private String driverClass;
    private long connectedAt;

    public String getId()              { return id; }
    public void setId(String v)        { id = v; }
    public String getName()            { return name; }
    public void setName(String v)      { name = v; }
    public String getJdbcUrl()         { return jdbcUrl; }
    public void setJdbcUrl(String v)   { jdbcUrl = v; }
    public String getUsername()        { return username; }
    public void setUsername(String v)  { username = v; }
    public String getDialect()         { return dialect; }
    public void setDialect(String v)   { dialect = v; }
    public String getStatus()          { return status; }
    public void setStatus(String v)    { status = v; }
    public String getDriverClass()     { return driverClass; }
    public void setDriverClass(String v){ driverClass = v; }
    public long getConnectedAt()       { return connectedAt; }
    public void setConnectedAt(long v) { connectedAt = v; }
}
