package com.hibernateide.model;

public class ConnectionConfig {
    private String name;
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClass;
    private String dialect;
    private boolean showSql = true;
    private boolean formatSql = false;

    public String getName()            { return name; }
    public void setName(String v)      { name = v; }
    public String getJdbcUrl()         { return jdbcUrl; }
    public void setJdbcUrl(String v)   { jdbcUrl = v; }
    public String getUsername()        { return username; }
    public void setUsername(String v)  { username = v; }
    public String getPassword()        { return password; }
    public void setPassword(String v)  { password = v; }
    public String getDriverClass()     { return driverClass; }
    public void setDriverClass(String v){ driverClass = v; }
    public String getDialect()         { return dialect; }
    public void setDialect(String v)   { dialect = v; }
    public boolean isShowSql()         { return showSql; }
    public void setShowSql(boolean v)  { showSql = v; }
    public boolean isFormatSql()       { return formatSql; }
    public void setFormatSql(boolean v){ formatSql = v; }

    public String resolveDriverClass() {
        if (driverClass != null && !driverClass.isBlank()) return driverClass;
        if (jdbcUrl == null) return null;
        if (jdbcUrl.startsWith("jdbc:mysql"))      return "com.mysql.cj.jdbc.Driver";
        if (jdbcUrl.startsWith("jdbc:postgresql"))  return "org.postgresql.Driver";
        if (jdbcUrl.startsWith("jdbc:h2"))          return "org.h2.Driver";
        if (jdbcUrl.startsWith("jdbc:hsqldb"))      return "org.hsqldb.jdbc.JDBCDriver";
        if (jdbcUrl.startsWith("jdbc:oracle"))      return "oracle.jdbc.OracleDriver";
        if (jdbcUrl.startsWith("jdbc:sqlserver"))   return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        if (jdbcUrl.startsWith("jdbc:db2"))         return "com.ibm.db2.jcc.DB2Driver";
        return null;
    }

    public String resolveDialect() {
        if (dialect != null && !dialect.isBlank()) return dialect;
        if (jdbcUrl == null) return "org.hibernate.dialect.GenericDialect";
        if (jdbcUrl.startsWith("jdbc:mysql"))       return "org.hibernate.dialect.MySQL5Dialect";
        if (jdbcUrl.startsWith("jdbc:postgresql"))  return "org.hibernate.dialect.PostgreSQLDialect";
        if (jdbcUrl.startsWith("jdbc:h2"))          return "org.hibernate.dialect.H2Dialect";
        if (jdbcUrl.startsWith("jdbc:hsqldb"))      return "org.hibernate.dialect.HSQLDialect";
        if (jdbcUrl.startsWith("jdbc:oracle"))      return "org.hibernate.dialect.Oracle9iDialect";
        if (jdbcUrl.startsWith("jdbc:sqlserver"))   return "org.hibernate.dialect.SQLServerDialect";
        if (jdbcUrl.startsWith("jdbc:db2"))         return "org.hibernate.dialect.DB2Dialect";
        return "org.hibernate.dialect.GenericDialect";
    }
}
