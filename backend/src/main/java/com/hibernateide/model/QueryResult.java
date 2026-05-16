package com.hibernateide.model;

import java.util.List;

public class QueryResult {
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
    private long executionTimeMs;
    private String queryType;
    private String generatedSql;
    private String message;
    private boolean success;
    private int affectedRows;

    public List<String> getColumns()              { return columns; }
    public void setColumns(List<String> v)        { columns = v; }
    public List<List<Object>> getRows()           { return rows; }
    public void setRows(List<List<Object>> v)     { rows = v; }
    public int getRowCount()                      { return rowCount; }
    public void setRowCount(int v)                { rowCount = v; }
    public long getExecutionTimeMs()              { return executionTimeMs; }
    public void setExecutionTimeMs(long v)        { executionTimeMs = v; }
    public String getQueryType()                  { return queryType; }
    public void setQueryType(String v)            { queryType = v; }
    public String getGeneratedSql()               { return generatedSql; }
    public void setGeneratedSql(String v)         { generatedSql = v; }
    public String getMessage()                    { return message; }
    public void setMessage(String v)              { message = v; }
    public boolean isSuccess()                    { return success; }
    public void setSuccess(boolean v)             { success = v; }
    public int getAffectedRows()                  { return affectedRows; }
    public void setAffectedRows(int v)            { affectedRows = v; }
}
