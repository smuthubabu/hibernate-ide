package com.hibernateide.service;

import com.hibernateide.model.EntityInfo;
import com.hibernateide.model.MappingPathRequest;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MappingService {

    private static final Logger log = LoggerFactory.getLogger(MappingService.class);

    @Autowired
    private ConnectionService connectionService;

    private final Map<String, List<EntityInfo>> connectionEntities = new ConcurrentHashMap<>();
    private final Map<String, ClassLoader>      connectionLoaders  = new ConcurrentHashMap<>();

    public List<EntityInfo> loadFromPath(String connectionId, MappingPathRequest req) throws Exception {
        // Build a single classloader from all provided classes/JAR paths
        ClassLoader classLoader = buildClassLoader(req.getClassesPaths());
        if (classLoader != null) connectionLoaders.put(connectionId, classLoader);

        // Collect HBM files from every provided path
        List<File> hbmFiles = new ArrayList<>();
        if (req.getHbmPaths() != null) {
            for (String path : req.getHbmPaths()) {
                hbmFiles.addAll(collectHbmFiles(path));
            }
        }
        if (hbmFiles.isEmpty())
            throw new IllegalArgumentException("No .hbm.xml files found in the provided path(s)");

        List<String> xmls = new ArrayList<>();
        for (File f : hbmFiles) {
            xmls.add(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8));
        }

        connectionService.setMappings(connectionId, xmls, connectionLoaders.get(connectionId));

        List<EntityInfo> entities = new ArrayList<>();
        for (File f : hbmFiles) entities.addAll(parseEntities(f));
        connectionEntities.put(connectionId, entities);

        log.info("Loaded {} HBM files → {} entities for connection={}", hbmFiles.size(), entities.size(), connectionId);
        return entities;
    }

    public List<EntityInfo> getEntities(String connectionId) {
        return connectionEntities.getOrDefault(connectionId, Collections.emptyList());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private ClassLoader buildClassLoader(List<String> classesPaths) throws Exception {
        if (classesPaths == null || classesPaths.isEmpty()) return null;
        List<URL> urls = new ArrayList<>();
        for (String path : classesPaths) {
            if (path == null || path.isBlank()) continue;
            File f = new File(path);
            if (!f.exists()) throw new IllegalArgumentException("Classes path does not exist: " + path);
            urls.add(f.toURI().toURL());
        }
        if (urls.isEmpty()) return null;
        return new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
    }

    private List<File> collectHbmFiles(String path) {
        if (path == null || path.isBlank()) return Collections.emptyList();
        File f = new File(path);
        if (!f.exists()) return Collections.emptyList();
        if (f.isFile() && f.getName().endsWith(".hbm.xml")) return Collections.singletonList(f);
        List<File> result = new ArrayList<>();
        if (f.isDirectory()) collectRecursive(f, result);
        return result;
    }

    private void collectRecursive(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) collectRecursive(f, out);
            else if (f.getName().endsWith(".hbm.xml")) out.add(f);
        }
    }

    private List<EntityInfo> parseEntities(File hbmFile) {
        try {
            SAXReader reader = new SAXReader();
            reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            reader.setFeature("http://xml.org/sax/features/validation", false);
            Document doc = reader.read(hbmFile);
            Element root = doc.getRootElement();
            String defaultSchema = root.attributeValue("schema");

            List<EntityInfo> entities = new ArrayList<>();
            for (Object o : root.elements("class")) {
                entities.add(parseClassElement((Element) o, defaultSchema));
            }
            return entities;
        } catch (Exception e) {
            log.warn("Failed to parse {}: {}", hbmFile.getName(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private EntityInfo parseClassElement(Element classEl, String defaultSchema) {
        EntityInfo entity = new EntityInfo();

        String className  = classEl.attributeValue("name");
        String entityName = classEl.attributeValue("entity-name");

        if (entityName != null) {
            entity.setEntityName(entityName);
        } else if (className != null) {
            int dot = className.lastIndexOf('.');
            entity.setEntityName(dot >= 0 ? className.substring(dot + 1) : className);
        }
        entity.setClassName(className);
        entity.setTableName(classEl.attributeValue("table"));
        entity.setSchemaName(classEl.attributeValue("schema") != null
                ? classEl.attributeValue("schema") : defaultSchema);

        // id
        Element idEl = classEl.element("id");
        if (idEl != null) {
            EntityInfo.PropertyInfo id = new EntityInfo.PropertyInfo();
            id.setName(idEl.attributeValue("name"));
            id.setColumn(idEl.attributeValue("column"));
            id.setType(idEl.attributeValue("type"));
            id.setNullable(false);
            entity.setIdProperty(id);
        }

        // scalar properties
        List<EntityInfo.PropertyInfo> props = new ArrayList<>();
        for (Object o : classEl.elements("property")) {
            Element propEl = (Element) o;
            EntityInfo.PropertyInfo prop = new EntityInfo.PropertyInfo();
            prop.setName(propEl.attributeValue("name"));
            prop.setColumn(propEl.attributeValue("column"));
            prop.setType(propEl.attributeValue("type"));
            prop.setNullable(!"false".equals(propEl.attributeValue("not-null")));
            props.add(prop);
        }
        entity.setProperties(props);

        // associations
        List<EntityInfo.AssociationInfo> assocs = new ArrayList<>();
        for (String assocType : Arrays.asList("many-to-one", "one-to-one", "one-to-many", "many-to-many")) {
            for (Object o : classEl.elements(assocType)) {
                Element el = (Element) o;
                EntityInfo.AssociationInfo assoc = new EntityInfo.AssociationInfo();
                assoc.setName(el.attributeValue("name"));
                assoc.setType(assocType);
                assoc.setColumn(el.attributeValue("column"));
                String targetClass = el.attributeValue("class");
                if (targetClass != null) {
                    int dot = targetClass.lastIndexOf('.');
                    assoc.setTargetEntity(dot >= 0 ? targetClass.substring(dot + 1) : targetClass);
                }
                assoc.setNullable(!"true".equals(el.attributeValue("not-null")));
                assocs.add(assoc);
            }
        }
        entity.setAssociations(assocs);

        return entity;
    }
}
