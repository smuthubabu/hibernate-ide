# Hibernate IDE

A web-based console for running HQL and SQL queries against live databases using **Hibernate 3.6.10.Final**.

![Stack](https://img.shields.io/badge/Hibernate-3.6-orange) ![Stack](https://img.shields.io/badge/Spring_Boot-2.x-green) ![Stack](https://img.shields.io/badge/React-18-blue)

## Features

- **Dual query modes** — switch between SQL and HQL in the same editor
- **HQL → SQL translation** — see the exact SQL Hibernate generates before running
- **Schema explorer** — browse tables, columns, and row counts; click to insert into the editor
- **Entity explorer** — load `.hbm.xml` mappings and browse entities with their properties and associations
- **HBM XML generator** — auto-generate Hibernate mapping XML from JDBC metadata, edit and register in one click
- **Smart autocomplete** — table/column suggestions in SQL mode, entity/property suggestions in HQL mode
- **Run selection** — select any portion of text and run only that; falls back to the full query when nothing is selected
- **Query history** — last 100 queries with one-click replay
- **Resizable sidebar** — drag to resize, collapse/expand with a panel toggle button
- **Dark / light theme** — Catppuccin-based themes
- **Multi-connection** — maintain multiple named database connections simultaneously, each with its own `SessionFactory`

## Supported Databases

MySQL · PostgreSQL · H2 · HSQLDB · Oracle · SQL Server · DB2 (Type 4 `jdbc:db2://host:port/db` and Type 2 `jdbc:db2:ALIAS`)

## Quick Start

```bash
./start.sh
```

Opens the backend on `:8080` and the frontend dev server on `:3000`. Navigate to **http://localhost:3000**.

### Prerequisites

- Java 11+, Maven 3.6+
- Node.js 16+, npm
- JDBC driver JAR for your database on the classpath (place in `backend/lib/` or add to `pom.xml`)

## Running Individually

```bash
# Backend only
cd backend && mvn spring-boot:run

# Frontend only
cd frontend && npm start

# Production frontend build
cd frontend && npm run build
```

### Loading External Class Mappings

When your `.hbm.xml` files reference actual Java entity classes (not dynamic-map mode), start the backend with those classes on the classpath:

```bash
java -cp "backend/target/hibernate-ide-backend-1.0.0.jar:/path/to/your/classes" \
     -Dloader.path="/path/to/your/classes" \
     org.springframework.boot.loader.JarLauncher
```

### DB2 Type 2

`start.sh` automatically sources the DB2 instance profile (`db2profile`) to set up `LD_LIBRARY_PATH`. Override the path:

```bash
DB2_PROFILE=/opt/ibm/db2/V11.5/db2profile ./start.sh
```

## Usage

1. **Connect** — expand the Connections panel, fill in JDBC URL / credentials, click Connect
2. **Browse schema** — switch to the Tables tab to explore tables and columns; click `▶` to generate a `SELECT *`, click `⊕` to generate and register an HBM mapping
3. **Load entities** — switch to the Entities tab, click the load button to point at `.hbm.xml` files or a directory
4. **Write queries** — use the SQL or HQL tab; autocomplete suggests tables/columns or entities/properties
5. **Run** — click **▶ Run** or press `Ctrl+Enter`; select text first to run only that portion
6. **Inspect generated SQL** — in HQL mode, click **</> Show SQL** to see the Hibernate-generated SQL and optionally copy or switch to it

## Architecture

```
Browser (React :3000)
  → axios → Spring Boot (:8080)
  → ConnectionService / QueryService / SchemaService / MappingService
  → Hibernate 3.6 SessionFactory (one per connection)
  → JDBC → database
```

Hibernate is managed entirely manually — Spring Boot's JPA/DataSource autoconfiguration is excluded. Each connection holds its own `SessionFactory` in dynamic-map entity mode (`Map<String,Object>`) unless external classes are loaded.

## Development

```bash
# Backend tests
cd backend && mvn test

# Frontend tests
cd frontend && npm test
```

The frontend dev server proxies all `/api/*` requests to `:8080` via the `"proxy"` field in `package.json`.
