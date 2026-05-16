package com.hibernateide.model;

import java.util.List;

public class TableInfo {
    private String tableName;
    private String tableType;
    private String schema;
    private String catalog;
    private List<ColumnInfo> columns;
    private List<String> primaryKeys;
    private long rowCount;

    public String getTableName()               { return tableName; }
    public void setTableName(String v)         { tableName = v; }
    public String getTableType()               { return tableType; }
    public void setTableType(String v)         { tableType = v; }
    public String getSchema()                  { return schema; }
    public void setSchema(String v)            { schema = v; }
    public String getCatalog()                 { return catalog; }
    public void setCatalog(String v)           { catalog = v; }
    public List<ColumnInfo> getColumns()       { return columns; }
    public void setColumns(List<ColumnInfo> v) { columns = v; }
    public List<String> getPrimaryKeys()       { return primaryKeys; }
    public void setPrimaryKeys(List<String> v) { primaryKeys = v; }
    public long getRowCount()                  { return rowCount; }
    public void setRowCount(long v)            { rowCount = v; }
}
