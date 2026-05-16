package com.hibernateide.service;

import com.hibernateide.model.QueryRequest;
import com.hibernateide.model.QueryResult;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.ast.ASTQueryTranslatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private ConnectionService connectionService;

    public QueryResult execute(QueryRequest request) {
        if ("HQL".equalsIgnoreCase(request.getQueryType())) {
            return executeHql(request);
        }
        return executeSql(request);
    }

    private QueryResult executeSql(QueryRequest request) {
        long start = System.currentTimeMillis();
        QueryResult result = new QueryResult();
        result.setQueryType("SQL");

        try (Connection conn = connectionService.getJdbcConnection(request.getConnectionId())) {
            String sql = request.getQuery().trim();
            String upperSql = sql.toUpperCase().replaceAll("\\s+", " ");

            if (upperSql.startsWith("SELECT") || upperSql.startsWith("WITH") || upperSql.startsWith("SHOW")) {
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }
                    result.setColumns(columns);

                    List<List<Object>> rows = new ArrayList<>();
                    int count = 0;
                    while (rs.next() && count < request.getMaxResults()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            Object val = rs.getObject(i);
                            row.add(val != null ? val.toString() : null);
                        }
                        rows.add(row);
                        count++;
                    }
                    result.setRows(rows);
                    result.setRowCount(rows.size());
                    result.setSuccess(true);
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    int affected = stmt.executeUpdate();
                    result.setAffectedRows(affected);
                    result.setColumns(Collections.singletonList("affected_rows"));
                    result.setRows(Collections.singletonList(Collections.singletonList(String.valueOf(affected))));
                    result.setRowCount(1);
                    result.setMessage(affected + " row(s) affected");
                    result.setSuccess(true);
                }
            }
        } catch (Exception e) {
            log.error("SQL execution failed", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - start);
        return result;
    }

    private QueryResult executeHql(QueryRequest request) {
        long start = System.currentTimeMillis();
        QueryResult result = new QueryResult();
        result.setQueryType("HQL");

        SessionFactory factory = connectionService.getSessionFactory(request.getConnectionId());
        Session session = factory.openSession();
        try {
            org.hibernate.Query query = session.createQuery(request.getQuery());
            query.setMaxResults(request.getMaxResults());
            if (request.getFirstResult() > 0) {
                query.setFirstResult(request.getFirstResult());
            }

            List<?> raw = query.list();
            if (raw.isEmpty()) {
                result.setColumns(new ArrayList<>());
                result.setRows(new ArrayList<>());
                result.setRowCount(0);
                result.setSuccess(true);
                result.setMessage("Query returned 0 rows");
            } else {
                Object first = raw.get(0);
                if (first instanceof Map) {
                    List<String> columns = new ArrayList<>(((Map<?, ?>) first).keySet().stream()
                            .map(Object::toString).collect(java.util.stream.Collectors.toList()));
                    List<List<Object>> rows = new ArrayList<>();
                    for (Object item : raw) {
                        Map<?, ?> map = (Map<?, ?>) item;
                        List<Object> row = new ArrayList<>();
                        for (String col : columns) {
                            Object val = map.get(col);
                            row.add(val != null ? val.toString() : null);
                        }
                        rows.add(row);
                    }
                    result.setColumns(columns);
                    result.setRows(rows);
                    result.setRowCount(rows.size());
                } else if (first instanceof Object[]) {
                    String[] aliases = query.getReturnAliases();
                    List<String> columns = aliases != null ? Arrays.asList(aliases) :
                            generateColumnNames(((Object[]) first).length);
                    List<List<Object>> rows = new ArrayList<>();
                    for (Object item : raw) {
                        Object[] arr = (Object[]) item;
                        List<Object> row = new ArrayList<>();
                        for (Object v : arr) row.add(v != null ? v.toString() : null);
                        rows.add(row);
                    }
                    result.setColumns(columns);
                    result.setRows(rows);
                    result.setRowCount(rows.size());
                } else {
                    result.setColumns(Collections.singletonList("result"));
                    List<List<Object>> rows = new ArrayList<>();
                    for (Object item : raw) {
                        rows.add(Collections.singletonList(item != null ? item.toString() : null));
                    }
                    result.setRows(rows);
                    result.setRowCount(rows.size());
                }
                result.setSuccess(true);
            }
        } catch (Exception e) {
            log.error("HQL execution failed", e);
            result.setSuccess(false);
            result.setMessage(e.getMessage());
        } finally {
            session.close();
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - start);
        return result;
    }

    public String explainHql(QueryRequest request) throws Exception {
        SessionFactory factory = connectionService.getSessionFactory(request.getConnectionId());
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) factory;
        String hql = request.getQuery().trim();

        // For simple "from EntityName" queries, auto-add join fetches so the SQL
        // shows the actual JOINs Hibernate fires (lazy associations only show in runtime queries)
        String enrichedHql = enrichHqlWithJoinFetches(hql, sfi);

        if (!enrichedHql.equals(hql)) {
            try {
                ASTQueryTranslatorFactory tf = new ASTQueryTranslatorFactory();
                QueryTranslator t = tf.createQueryTranslator(enrichedHql, enrichedHql,
                        Collections.emptyMap(), sfi);
                t.compile(Collections.emptyMap(), false);
                return t.getSQLString();
            } catch (Exception e) {
                log.debug("Enriched HQL failed, falling back to original: {}", e.getMessage());
            }
        }

        ASTQueryTranslatorFactory tf = new ASTQueryTranslatorFactory();
        QueryTranslator translator = tf.createQueryTranslator(hql, hql,
                Collections.emptyMap(), sfi);
        translator.compile(Collections.emptyMap(), false);
        return translator.getSQLString();
    }

    @SuppressWarnings("unchecked")
    private String enrichHqlWithJoinFetches(String hql, SessionFactoryImplementor sfi) {
        try {
            String upper = hql.toUpperCase().trim();
            // Only enrich simple from-clauses; skip SELECT projections, explicit JOINs, WHERE
            if (upper.startsWith("SELECT") || upper.contains(" JOIN ") || upper.contains(" WHERE ")) {
                return hql;
            }

            // Strip trailing ORDER BY for pattern matching, restore later
            String orderByClause = "";
            int orderByIdx = upper.indexOf(" ORDER BY ");
            String hqlBase = hql;
            if (orderByIdx >= 0) {
                orderByClause = " " + hql.substring(orderByIdx + 1).trim();
                hqlBase = hql.substring(0, orderByIdx).trim();
            }

            // Match: FROM EntityName [alias]
            Pattern p = Pattern.compile("(?i)^from\\s+(\\S+)(?:\\s+(\\w+))?\\s*$");
            Matcher m = p.matcher(hqlBase);
            if (!m.matches()) return hql;

            String entityName = m.group(1);
            String alias = m.group(2) != null ? m.group(2) : "e0_x";

            // Find class metadata (try exact name then short-name match)
            org.hibernate.metadata.ClassMetadata meta = sfi.getClassMetadata(entityName);
            if (meta == null) {
                for (Map.Entry<?, ?> entry : sfi.getAllClassMetadata().entrySet()) {
                    String key = entry.getKey().toString();
                    if (key.equals(entityName) || key.endsWith("." + entityName)) {
                        meta = (org.hibernate.metadata.ClassMetadata) entry.getValue();
                        break;
                    }
                }
            }
            if (meta == null) return hql;

            String[] propNames = meta.getPropertyNames();
            org.hibernate.type.Type[] propTypes = meta.getPropertyTypes();

            StringBuilder joins = new StringBuilder();
            for (int i = 0; i < propNames.length; i++) {
                if (propTypes[i] instanceof org.hibernate.type.EntityType) {
                    joins.append(" left join fetch ").append(alias).append(".").append(propNames[i]);
                }
            }
            if (joins.length() == 0) return hql;

            return "from " + entityName + " " + alias + joins + orderByClause;
        } catch (Exception e) {
            log.debug("Could not enrich HQL with join fetches: {}", e.getMessage());
            return hql;
        }
    }

    private List<String> generateColumnNames(int count) {
        List<String> cols = new ArrayList<>();
        for (int i = 1; i <= count; i++) cols.add("col" + i);
        return cols;
    }
}
