package com.hibernateide.model;

public class ColumnInfo {
    private String columnName;
    private String dataType;
    private int columnSize;
    private boolean nullable;
    private String defaultValue;
    private boolean primaryKey;
    private boolean autoIncrement;
    private int ordinalPosition;

    public String getColumnName()          { return columnName; }
    public void setColumnName(String v)    { columnName = v; }
    public String getDataType()            { return dataType; }
    public void setDataType(String v)      { dataType = v; }
    public int getColumnSize()             { return columnSize; }
    public void setColumnSize(int v)       { columnSize = v; }
    public boolean isNullable()            { return nullable; }
    public void setNullable(boolean v)     { nullable = v; }
    public String getDefaultValue()        { return defaultValue; }
    public void setDefaultValue(String v)  { defaultValue = v; }
    public boolean isPrimaryKey()          { return primaryKey; }
    public void setPrimaryKey(boolean v)   { primaryKey = v; }
    public boolean isAutoIncrement()       { return autoIncrement; }
    public void setAutoIncrement(boolean v){ autoIncrement = v; }
    public int getOrdinalPosition()        { return ordinalPosition; }
    public void setOrdinalPosition(int v)  { ordinalPosition = v; }
}
