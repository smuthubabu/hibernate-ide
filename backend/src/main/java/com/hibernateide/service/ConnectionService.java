package com.hibernateide.service;

import com.hibernateide.model.ConnectionConfig;
import com.hibernateide.model.ConnectionInfo;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import org.xml.sax.InputSource;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionService {

    private static final Logger log = LoggerFactory.getLogger(ConnectionService.class);

    private final Map<String, SessionFactory>   sessionFactories      = new ConcurrentHashMap<>();
    private final Map<String, ConnectionConfig>  connectionConfigs     = new ConcurrentHashMap<>();
    private final Map<String, ConnectionInfo>    connectionInfos       = new ConcurrentHashMap<>();
    private final Map<String, List<String>>      connectionXmlMappings = new ConcurrentHashMap<>();

    public ConnectionInfo createConnection(ConnectionConfig config) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String resolvedDriver = config.resolveDriverClass();
        String resolvedDialect = config.resolveDialect();

        if (resolvedDriver == null) {
            throw new IllegalArgumentException("Cannot determine JDBC driver for URL: " + config.getJdbcUrl());
        }

        Configuration hibernateConfig = buildConfiguration(config, resolvedDriver, resolvedDialect);
        SessionFactory factory = hibernateConfig.buildSessionFactory();

        sessionFactories.put(id, factory);
        connectionConfigs.put(id, config);
        connectionXmlMappings.put(id, new ArrayList<>());

        ConnectionInfo info = new ConnectionInfo();
        info.setId(id);
        info.setName(config.getName() != null ? config.getName() : id);
        info.setJdbcUrl(config.getJdbcUrl());
        info.setUsername(config.getUsername());
        info.setDialect(resolvedDialect);
        info.setDriverClass(resolvedDriver);
        info.setStatus("connected");
        info.setConnectedAt(System.currentTimeMillis());
        connectionInfos.put(id, info);

        log.info("Connection created: id={} url={} dialect={}", id, config.getJdbcUrl(), resolvedDialect);
        return info;
    }

    public ConnectionInfo testConnection(ConnectionConfig config) {
        String resolvedDriver = config.resolveDriverClass();
        if (resolvedDriver == null) {
            throw new IllegalArgumentException("Cannot determine JDBC driver for URL: " + config.getJdbcUrl());
        }
        try {
            Class.forName(resolvedDriver);
            try (Connection c = DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword())) {
                String dbProduct = c.getMetaData().getDatabaseProductName();
                String dbVersion = c.getMetaData().getDatabaseProductVersion();
                ConnectionInfo info = new ConnectionInfo();
                info.setStatus("ok");
                info.setDialect(config.resolveDialect());
                info.setDriverClass(resolvedDriver);
                info.setName(dbProduct + " " + dbVersion);
                return info;
            }
        } catch (Exception e) {
            throw new RuntimeException("Connection test failed: " + e.getMessage(), e);
        }
    }

    public void addMapping(String connectionId, String hbmXml) {
        addMappings(connectionId, Collections.singletonList(hbmXml), null);
    }

    public void addMappings(String connectionId, List<String> hbmXmls, ClassLoader classLoader) {
        rebuildWithMappings(connectionId, hbmXmls, classLoader, false);
    }

    /** Replace all mappings for this connection (used when loading from path — idempotent on retry). */
    public void setMappings(String connectionId, List<String> hbmXmls, ClassLoader classLoader) {
        rebuildWithMappings(connectionId, hbmXmls, classLoader, true);
    }

    private void rebuildWithMappings(String connectionId, List<String> hbmXmls, ClassLoader classLoader, boolean replace) {
        SessionFactory old = sessionFactories.remove(connectionId);
        if (old != null) old.close();

        ConnectionConfig config = connectionConfigs.get(connectionId);
        if (config == null) throw new IllegalArgumentException("Unknown connection: " + connectionId);

        List<String> mappings;
        if (replace) {
            mappings = new ArrayList<>(hbmXmls);
            connectionXmlMappings.put(connectionId, mappings);
        } else {
            mappings = connectionXmlMappings.computeIfAbsent(connectionId, k -> new ArrayList<>());
            mappings.addAll(hbmXmls);
        }

        ClassLoader prev = Thread.currentThread().getContextClassLoader();
        try {
            if (classLoader != null) Thread.currentThread().setContextClassLoader(classLoader);
            Configuration hibernateConfig = buildConfiguration(config, config.resolveDriverClass(), config.resolveDialect());
            hibernateConfig.setEntityResolver((publicId, systemId) -> {
                if (systemId != null && systemId.contains("hibernate.sourceforge.net/")) {
                    String dtdName = systemId.substring(systemId.lastIndexOf('/') + 1);
                    InputStream stream = org.hibernate.cfg.Configuration.class.getClassLoader()
                            .getResourceAsStream("org/hibernate/" + dtdName);
                    if (stream != null) return new InputSource(stream);
                }
                return null;
            });
            for (String xml : mappings) hibernateConfig.addXML(xml);
            sessionFactories.put(connectionId, hibernateConfig.buildSessionFactory());
        } catch (Exception e) {
            // Roll back accumulation so retrying doesn't add duplicates
            if (!replace) mappings.subList(mappings.size() - hbmXmls.size(), mappings.size()).clear();
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }
        log.info("Mappings {} ({}) and SessionFactory rebuilt for connection={}",
                replace ? "set" : "added", hbmXmls.size(), connectionId);
    }

    public void removeConnection(String id) {
        SessionFactory factory = sessionFactories.remove(id);
        if (factory != null) factory.close();
        connectionConfigs.remove(id);
        connectionInfos.remove(id);
        connectionXmlMappings.remove(id);
        log.info("Connection removed: id={}", id);
    }

    public List<ConnectionInfo> listConnections() {
        return new ArrayList<>(connectionInfos.values());
    }

    public SessionFactory getSessionFactory(String id) {
        SessionFactory factory = sessionFactories.get(id);
        if (factory == null) throw new IllegalArgumentException("No active connection with id: " + id);
        return factory;
    }

    public ConnectionConfig getConfig(String id) {
        ConnectionConfig config = connectionConfigs.get(id);
        if (config == null) throw new IllegalArgumentException("No connection config for id: " + id);
        return config;
    }

    public boolean hasConnection(String id) {
        return sessionFactories.containsKey(id);
    }

    public ConnectionInfo getConnectionInfo(String id) {
        return connectionInfos.get(id);
    }

    public Connection getJdbcConnection(String id) throws Exception {
        ConnectionConfig config = getConfig(id);
        Class.forName(config.resolveDriverClass());
        return DriverManager.getConnection(config.getJdbcUrl(), config.getUsername(), config.getPassword());
    }

    private Configuration buildConfiguration(ConnectionConfig config, String driver, String dialect) {
        Configuration cfg = new Configuration();
        cfg.setProperty("hibernate.connection.driver_class", driver);
        cfg.setProperty("hibernate.connection.url", config.getJdbcUrl());
        cfg.setProperty("hibernate.connection.username", config.getUsername() != null ? config.getUsername() : "");
        cfg.setProperty("hibernate.connection.password", config.getPassword() != null ? config.getPassword() : "");
        cfg.setProperty("hibernate.dialect", dialect);
        cfg.setProperty("hibernate.show_sql", String.valueOf(config.isShowSql()));
        cfg.setProperty("hibernate.format_sql", String.valueOf(config.isFormatSql()));
        cfg.setProperty("hibernate.connection.pool_size", "5");
        cfg.setProperty("hibernate.current_session_context_class", "thread");
        // Dynamic-map entity mode: entities are accessed as Map<String,Object> — no Java POJO classes needed
        cfg.setProperty("hibernate.default_entity_mode", "dynamic-map");
        return cfg;
    }
}
