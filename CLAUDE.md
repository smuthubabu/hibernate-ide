# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Hibernate IDE is a web-based console for running HQL and SQL queries against live databases using **Hibernate 3.6.10.Final** — deliberately pinned to 3.x, not upgraded. The backend manages multiple simultaneous `SessionFactory` instances (one per connection), all operating in **dynamic-map entity mode** (entities as `Map<String,Object>`, no Java POJOs required at runtime unless loading external class mappings).

## Commands

### Start everything (dev)
```bash
./start.sh          # backend :8080 + frontend dev server :3000
```

### Backend only
```bash
cd backend
mvn spring-boot:run                     # standard start
mvn clean package -DskipTests          # build JAR

# Start with external classes on classpath (required when loading .hbm.xml
# files that reference actual Java classes not in the IDE JAR):
java -cp "target/hibernate-ide-backend-1.0.0.jar:/path/to/external/classes" \
     -Dloader.path="/path/to/external/classes" \
     org.springframework.boot.loader.JarLauncher
```

### Frontend only
```bash
cd frontend
npm start           # dev server :3000 (proxies /api → :8080)
npm run build       # production build into frontend/build/
```

### Tests
```bash
cd backend && mvn test                  # backend (currently minimal)
cd frontend && npm test                 # React tests
```

## Architecture

### Request flow
```
Browser (React :3000)
  → axios (proxied via package.json "proxy": "http://localhost:8080")
  → Spring Boot REST controllers (:8080)
  → ConnectionService / QueryService / SchemaService / MappingService
  → Hibernate 3.6 SessionFactory (one per active connection, held in-memory)
  → JDBC driver → database
```

### Backend — key design decisions

**ConnectionService** is the central state store. It holds four concurrent maps keyed by connection ID (8-char UUID prefix):
- `sessionFactories` — live Hibernate `SessionFactory` per connection
- `connectionConfigs` — original `ConnectionConfig` per connection
- `connectionInfos` — display metadata per connection
- `connectionMappings` — accumulated HBM XML strings per connection

Every time a mapping is added, `addMappings()` closes the old `SessionFactory`, rebuilds `Configuration` from scratch with all accumulated XMLs, and opens a new one. This is intentional — Hibernate 3 `Configuration` is immutable after `buildSessionFactory()`.

When loading external class mappings (`MappingService.loadFromPath`), a `URLClassLoader` is set as `Thread.currentThread().getContextClassLoader()` before calling `buildSessionFactory()`, then restored. This is how Hibernate 3 resolves entity classes.

**QueryService.executeHql()** does **not** use `Transformers.ALIAS_TO_ENTITY_MAP` — that transformer throws when aliases are null (which happens on `from EntityName` queries without a SELECT clause). Results are handled by type: `Map` (dynamic-map entity), `Object[]` (projection), or scalar.

**QueryService.explainHql()** uses `ASTQueryTranslatorFactory` to translate HQL → SQL without executing the query. Requires a live `SessionFactory` with mappings registered.

**MappingService** parses HBM XML files using dom4j `SAXReader` with DTD validation disabled (external DTD fetch would fail). Entity metadata extracted here populates the Entities tree in the UI.

**ConnectionConfig.resolveDriverClass() / resolveDialect()** auto-detect from JDBC URL prefix. Supported: MySQL, PostgreSQL, H2, HSQLDB, Oracle, SQL Server, DB2 (both Type 2 `jdbc:db2:ALIAS` and Type 4 `jdbc:db2://host:port/db`).

### Frontend — key design decisions

**State lifted to App.js**: `queryType` (`'SQL'`|`'HQL'`), `currentQuery`, `sidebarWidth`/`collapsed` (resizable sidebar). `queryType` is passed down to `QueryEditor` as a prop so `EntityExplorer` can switch it to `'HQL'` when an entity is clicked (`handleEntitySelect`).

**Three sidebar tabs**: Entities (HBM-mapped objects, searchable, loads via `MappingService`), Tables (JDBC metadata via `SchemaService`), History.

**EntityExplorer** owns the "Load Mappings" modal. Clicking an entity name or its HQL button calls `onEntitySelect(entityName)` in App.js, which sets `currentQuery = "from EntityName"` and `queryType = "HQL"`.

**QueryEditor** Show SQL button calls `POST /api/query/explain` and shows the Hibernate-generated SQL in a modal with a "Use as SQL" option that copies the SQL into the editor and switches to SQL mode.

The frontend dev proxy (`"proxy": "http://localhost:8080"` in `package.json`) means all `/api/*` calls from the React dev server forward to the backend automatically.

## API Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/connections` | Create connection + SessionFactory |
| POST | `/api/connections/test` | Test connection without storing |
| DELETE | `/api/connections/{id}` | Close SessionFactory + remove |
| POST | `/api/connections/{id}/mappings` | Register single HBM XML string |
| POST | `/api/connections/{id}/mappings/path` | Load HBM files from disk paths + optional classes/JARs |
| GET | `/api/schema/{id}/tables` | JDBC metadata table list |
| GET | `/api/schema/{id}/tables/{table}` | Columns + row count for one table |
| GET | `/api/schema/{id}/tables/{table}/hbm` | Generate HBM XML from JDBC metadata |
| GET | `/api/schema/{id}/entities` | Return parsed entity metadata (from MappingService) |
| POST | `/api/query/execute` | Run SQL or HQL, return rows + columns |
| POST | `/api/query/explain` | Translate HQL → SQL via ASTQueryTranslatorFactory (no execution) |

## Hibernate 3 Constraints

- Hibernate version is locked at `3.6.10.Final` — do not upgrade to 4.x/5.x as the API differs significantly.
- `hibernate.default_entity_mode=dynamic-map` is always set; entities are `Map<String,Object>` unless external classes are loaded.
- `hibernate.connection.pool_size=5` and `hibernate.current_session_context_class=thread` are always set.
- Spring Boot's DataSource and JPA autoconfiguration are **excluded** — Hibernate is managed entirely manually.
- The `dom4j:dom4j:1.6.1` dependency (not `org.dom4j`) is required by Hibernate 3.
- DB2 Type 2 (`jdbc:db2:ALIAS`) requires IBM DB2 client native libraries on the host OS; the JCC JAR alone only supports Type 4.
