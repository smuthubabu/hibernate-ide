package com.hibernateide.model;

import java.util.List;

public class EntityInfo {
    private String entityName;   // simple name used in HQL (e.g. "ReportDefinitionBO")
    private String className;    // fully-qualified class name, null when entity-name only
    private String tableName;
    private String schemaName;
    private PropertyInfo idProperty;
    private List<PropertyInfo> properties;
    private List<AssociationInfo> associations;

    public String getEntityName()                   { return entityName; }
    public void setEntityName(String v)             { entityName = v; }
    public String getClassName()                    { return className; }
    public void setClassName(String v)              { className = v; }
    public String getTableName()                    { return tableName; }
    public void setTableName(String v)              { tableName = v; }
    public String getSchemaName()                   { return schemaName; }
    public void setSchemaName(String v)             { schemaName = v; }
    public PropertyInfo getIdProperty()             { return idProperty; }
    public void setIdProperty(PropertyInfo v)       { idProperty = v; }
    public List<PropertyInfo> getProperties()       { return properties; }
    public void setProperties(List<PropertyInfo> v) { properties = v; }
    public List<AssociationInfo> getAssociations()  { return associations; }
    public void setAssociations(List<AssociationInfo> v) { associations = v; }

    public static class PropertyInfo {
        private String name;
        private String column;
        private String type;
        private boolean nullable = true;

        public String getName()          { return name; }
        public void setName(String v)    { name = v; }
        public String getColumn()        { return column; }
        public void setColumn(String v)  { column = v; }
        public String getType()          { return type; }
        public void setType(String v)    { type = v; }
        public boolean isNullable()      { return nullable; }
        public void setNullable(boolean v){ nullable = v; }
    }

    public static class AssociationInfo {
        private String name;
        private String targetEntity;
        private String column;
        private String type;   // many-to-one | one-to-one | one-to-many | many-to-many
        private boolean nullable = true;

        public String getName()            { return name; }
        public void setName(String v)      { name = v; }
        public String getTargetEntity()    { return targetEntity; }
        public void setTargetEntity(String v){ targetEntity = v; }
        public String getColumn()          { return column; }
        public void setColumn(String v)    { column = v; }
        public String getType()            { return type; }
        public void setType(String v)      { type = v; }
        public boolean isNullable()        { return nullable; }
        public void setNullable(boolean v) { nullable = v; }
    }
}
