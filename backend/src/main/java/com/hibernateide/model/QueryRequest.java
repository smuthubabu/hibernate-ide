package com.hibernateide.model;

public class QueryRequest {
    private String connectionId;
    private String query;
    private String queryType = "SQL";
    private int maxResults = 500;
    private int firstResult = 0;

    public String getConnectionId()         { return connectionId; }
    public void setConnectionId(String v)   { connectionId = v; }
    public String getQuery()                { return query; }
    public void setQuery(String v)          { query = v; }
    public String getQueryType()            { return queryType; }
    public void setQueryType(String v)      { queryType = v; }
    public int getMaxResults()              { return maxResults; }
    public void setMaxResults(int v)        { maxResults = v; }
    public int getFirstResult()             { return firstResult; }
    public void setFirstResult(int v)       { firstResult = v; }
}
