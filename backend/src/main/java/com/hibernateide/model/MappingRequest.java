package com.hibernateide.model;

public class MappingRequest {
    private String connectionId;
    private String mappingName;
    private String hbmXml;

    public String getConnectionId()        { return connectionId; }
    public void setConnectionId(String v)  { connectionId = v; }
    public String getMappingName()         { return mappingName; }
    public void setMappingName(String v)   { mappingName = v; }
    public String getHbmXml()             { return hbmXml; }
    public void setHbmXml(String v)       { hbmXml = v; }
}
