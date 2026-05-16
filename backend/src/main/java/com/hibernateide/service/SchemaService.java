package com.hibernateide.service;

import com.hibernateide.model.ColumnInfo;
import com.hibernateide.model.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class SchemaService {

    private static final Logger log = LoggerFactory.getLogger(SchemaService.class);

    @Autowired
    private ConnectionService connectionService;

    public List<TableInfo> getTables(String connectionId) throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = getDefaultSchema(conn);

            try (ResultSet rs = meta.getTables(catalog, schema, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    TableInfo table = new TableInfo();
                    table.setTableName(rs.getString("TABLE_NAME"));
                    table.setTableType(rs.getString("TABLE_TYPE"));
                    table.setCatalog(rs.getString("TABLE_CAT"));
                    table.setSchema(rs.getString("TABLE_SCHEM"));
                    tables.add(table);
                }
            }
        }
        return tables;
    }

    public TableInfo getTableDetail(String connectionId, String tableName) throws Exception {
        try (Connection conn = connectionService.getJdbcConnection(connectionId)) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schema = getDefaultSchema(conn);

            TableInfo table = new TableInfo();
            table.setTableName(tableName);

            Set<String> primaryKeys = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(catalog, schema, tableName)) {
                while (pkRs.next()) primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
            table.setPrimaryKeys(new ArrayList<>(primaryKeys));

            List<ColumnInfo> columns = new ArrayList<>();
            try (ResultSet colRs = meta.getColumns(catalog, schema, tableName, "%")) {
                while (colRs.next()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(colRs.getString("COLUMN_NAME"));
                    col.setDataType(colRs.getString("TYPE_NAME"));
                    col.setColumnSize(colRs.getInt("COLUMN_SIZE"));
                    col.setNullable(colRs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls);
                    col.setDefaultValue(colRs.getString("COLUMN_DEF"));
                    col.setOrdinalPosition(colRs.getInt("ORDINAL_POSITION"));
                    col.setPrimaryKey(primaryKeys.contains(col.getColumnName()));
                    String isAutoIncrement = colRs.getString("IS_AUTOINCREMENT");
                    col.setAutoIncrement("YES".equalsIgnoreCase(isAutoIncrement));
                    columns.add(col);
                }
            }
            columns.sort(Comparator.comparingInt(ColumnInfo::getOrdinalPosition));
            table.setColumns(columns);

            try {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) table.setRowCount(rs.getLong(1));
                }
            } catch (Exception e) {
                log.debug("Could not get row count for {}: {}", tableName, e.getMessage());
            }

            return table;
        }
    }

    public String generateHbmXml(String connectionId, String tableName, String className) throws Exception {
        TableInfo table = getTableDetail(connectionId, tableName);
        String simpleClassName = className != null && !className.isBlank() ? className : toCamelCase(tableName);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE hibernate-mapping PUBLIC\n");
        sb.append("    \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n");
        sb.append("    \"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n");
        sb.append("<hibernate-mapping>\n");
        sb.append("    <class entity-name=\"").append(simpleClassName).append("\" table=\"").append(tableName).append("\">\n");
        sb.append("        <tuplizer entity-mode=\"dynamic-map\" class=\"org.hibernate.tuple.entity.DynamicMapEntityTuplizer\"/>\n");

        boolean idDone = false;
        for (ColumnInfo col : table.getColumns()) {
            if (col.isPrimaryKey() && !idDone) {
                sb.append("        <id name=\"").append(toCamelCase(col.getColumnName())).append("\"");
                sb.append(" column=\"").append(col.getColumnName()).append("\"");
                sb.append(" type=\"").append(sqlTypeToHibernate(col.getDataType())).append("\">\n");
                if (col.isAutoIncrement()) {
                    sb.append("            <generator class=\"native\"/>\n");
                } else {
                    sb.append("            <generator class=\"assigned\"/>\n");
                }
                sb.append("        </id>\n");
                idDone = true;
            } else if (!col.isPrimaryKey()) {
                sb.append("        <property name=\"").append(toCamelCase(col.getColumnName())).append("\"");
                sb.append(" column=\"").append(col.getColumnName()).append("\"");
                sb.append(" type=\"").append(sqlTypeToHibernate(col.getDataType())).append("\"");
                if (!col.isNullable()) sb.append(" not-null=\"true\"");
                sb.append("/>\n");
            }
        }
        sb.append("    </class>\n");
        sb.append("</hibernate-mapping>\n");
        return sb.toString();
    }

    private String getDefaultSchema(Connection conn) {
        try {
            return conn.getSchema();
        } catch (Exception e) {
            return null;
        }
    }

    private String toCamelCase(String snake) {
        if (snake == null) return "entity";
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : sb.length() == 0 ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    private String sqlTypeToHibernate(String sqlType) {
        if (sqlType == null) return "string";
        switch (sqlType.toUpperCase()) {
            case "INT": case "INTEGER": case "INT4": return "integer";
            case "BIGINT": case "INT8": return "long";
            case "SMALLINT": case "INT2": return "short";
            case "FLOAT": case "REAL": return "float";
            case "DOUBLE": case "DOUBLE PRECISION": return "double";
            case "DECIMAL": case "NUMERIC": return "big_decimal";
            case "BOOLEAN": case "BOOL": return "boolean";
            case "DATE": return "date";
            case "TIME": return "time";
            case "TIMESTAMP": case "DATETIME": return "timestamp";
            case "CLOB": case "TEXT": case "LONGTEXT": return "text";
            case "BLOB": case "BYTEA": return "binary";
            default: return "string";
        }
    }
}
