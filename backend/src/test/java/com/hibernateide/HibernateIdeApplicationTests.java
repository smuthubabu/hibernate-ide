package com.hibernateide;

import com.hibernateide.model.*;
import com.hibernateide.service.ConnectionService;
import com.hibernateide.service.QueryService;
import com.hibernateide.service.SchemaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HibernateIdeApplicationTests {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private SchemaService schemaService;

    private String connectionId;

    @BeforeEach
    void setUp() {
        ConnectionConfig config = new ConnectionConfig();
        config.setName("test-h2");
        // Unique DB per test class instance to avoid cross-test leakage
        config.setJdbcUrl("jdbc:h2:mem:testdb_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
        ConnectionInfo info = connectionService.createConnection(config);
        connectionId = info.getId();

        exec("DROP TABLE IF EXISTS employees");
        exec("CREATE TABLE employees " +
                "(id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100), department VARCHAR(50), salary DECIMAL(10,2))");
        exec("INSERT INTO employees (name, department, salary) VALUES " +
                "('Alice', 'Engineering', 95000), ('Bob', 'Marketing', 72000), ('Carol', 'Engineering', 88000)");
    }

    @AfterEach
    void tearDown() {
        if (connectionId != null) {
            exec("DROP TABLE IF EXISTS employees");
            connectionService.removeConnection(connectionId);
        }
    }

    private QueryResult exec(String sql) {
        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("SQL");
        req.setQuery(sql);
        return queryService.execute(req);
    }

    @Test
    void contextLoads() {
        assertThat(connectionService).isNotNull();
        assertThat(queryService).isNotNull();
        assertThat(schemaService).isNotNull();
    }

    @Test
    void testConnection_isListed() {
        List<ConnectionInfo> connections = connectionService.listConnections();
        assertThat(connections).isNotEmpty();
        assertThat(connections.stream().anyMatch(c -> c.getId().equals(connectionId))).isTrue();
    }

    @Test
    void testSqlSelectQuery() {
        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("SQL");
        req.setQuery("SELECT * FROM employees ORDER BY id");
        req.setMaxResults(100);

        QueryResult result = queryService.execute(req);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getColumns()).isNotEmpty();
        assertThat(result.getRows()).hasSize(3);
    }

    @Test
    void testSqlFilterQuery() {
        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("SQL");
        req.setQuery("SELECT name, salary FROM employees WHERE department = 'Engineering'");
        req.setMaxResults(100);

        QueryResult result = queryService.execute(req);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRows()).hasSize(2);
        assertThat(result.getColumns()).hasSize(2);
    }

    @Test
    void testSqlAggregateQuery() {
        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("SQL");
        req.setQuery("SELECT department, COUNT(*) as cnt, AVG(salary) as avg_salary FROM employees GROUP BY department");
        req.setMaxResults(100);

        QueryResult result = queryService.execute(req);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRows()).hasSize(2);
    }

    @Test
    void testSqlInsertUpdateDelete() {
        QueryResult insertResult = exec("INSERT INTO employees (name, department, salary) VALUES ('Dave', 'HR', 65000)");
        assertThat(insertResult.isSuccess()).isTrue();
        assertThat(insertResult.getAffectedRows()).isEqualTo(1);

        QueryResult updateResult = exec("UPDATE employees SET salary = 70000 WHERE name = 'Dave'");
        assertThat(updateResult.isSuccess()).isTrue();
        assertThat(updateResult.getAffectedRows()).isEqualTo(1);

        QueryResult deleteResult = exec("DELETE FROM employees WHERE name = 'Dave'");
        assertThat(deleteResult.isSuccess()).isTrue();
        assertThat(deleteResult.getAffectedRows()).isEqualTo(1);
    }

    @Test
    void testSchemaTableList() throws Exception {
        List<TableInfo> tables = schemaService.getTables(connectionId);
        assertThat(tables).isNotEmpty();
        assertThat(tables.stream().anyMatch(t -> t.getTableName().equalsIgnoreCase("employees"))).isTrue();
    }

    @Test
    void testSchemaTableDetail() throws Exception {
        TableInfo detail = schemaService.getTableDetail(connectionId, "EMPLOYEES");
        assertThat(detail).isNotNull();
        assertThat(detail.getColumns()).isNotEmpty();
        assertThat(detail.getColumns().stream().anyMatch(c -> c.getColumnName().equalsIgnoreCase("name"))).isTrue();
    }

    @Test
    void testHbmGeneration() throws Exception {
        String hbm = schemaService.generateHbmXml(connectionId, "EMPLOYEES", "Employee");
        assertThat(hbm).contains("hibernate-mapping");
        assertThat(hbm).contains("entity-name=\"Employee\"");
        assertThat(hbm).contains("table=\"EMPLOYEES\"");
        assertThat(hbm).contains("dynamic-map");
    }

    @Test
    void testConnectionTest() {
        ConnectionConfig config = new ConnectionConfig();
        config.setJdbcUrl("jdbc:h2:mem:probe_" + System.nanoTime());
        config.setUsername("sa");
        config.setPassword("");
        ConnectionInfo info = connectionService.testConnection(config);
        assertThat(info.getStatus()).isEqualTo("ok");
    }

    @Test
    void testHqlQueryWithMapping() {
        // Dynamic-map entity mode: no Java POJO class needed, entity-name is used directly
        String hbmXml =
                "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE hibernate-mapping PUBLIC \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n" +
                "\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n" +
                "<hibernate-mapping>\n" +
                "  <class entity-name=\"Employee\" table=\"EMPLOYEES\">\n" +
                "    <tuplizer entity-mode=\"dynamic-map\" class=\"org.hibernate.tuple.entity.DynamicMapEntityTuplizer\"/>\n" +
                "    <id name=\"id\" column=\"ID\" type=\"integer\"><generator class=\"native\"/></id>\n" +
                "    <property name=\"name\" column=\"NAME\" type=\"string\"/>\n" +
                "    <property name=\"department\" column=\"DEPARTMENT\" type=\"string\"/>\n" +
                "    <property name=\"salary\" column=\"SALARY\" type=\"big_decimal\"/>\n" +
                "  </class>\n" +
                "</hibernate-mapping>";

        connectionService.addMapping(connectionId, hbmXml);

        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("HQL");
        req.setQuery("SELECT e.name as name, e.department as department FROM Employee e WHERE e.salary > 80000");
        req.setMaxResults(100);

        QueryResult result = queryService.execute(req);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRows()).isNotEmpty();
    }

    @Test
    void testExecutionTimeTracked() {
        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("SQL");
        req.setQuery("SELECT COUNT(*) FROM employees");
        req.setMaxResults(1);

        QueryResult result = queryService.execute(req);
        assertThat(result.getExecutionTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testMaxResultsLimit() {
        QueryRequest req = new QueryRequest();
        req.setConnectionId(connectionId);
        req.setQueryType("SQL");
        req.setQuery("SELECT * FROM employees");
        req.setMaxResults(1);

        QueryResult result = queryService.execute(req);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getRows().size()).isLessThanOrEqualTo(1);
    }
}
