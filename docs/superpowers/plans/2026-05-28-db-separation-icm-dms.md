# DB Separation: ICM and DMS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give ICM and DMS each their own PostgreSQL database, remove `doc_management_user`, and replace cluster-level `ALTER ROLE search_path` with JDBC connection-level `currentSchema`.

**Architecture:** ICM keeps `doc_management_db`, DMS moves to a new `dms_db`. The shared Postgres container (ICM-owned docker-compose) initialises `dms_db` via a simplified init script. Each service creates its own schema before Liquibase runs: ICM via `spring.sql.init`, DMS via its existing `preliquibase` script. `search_path` is set per-connection via the JDBC `currentSchema` parameter, eliminating the `ALTER ROLE` pattern that caused cross-service interference.

**Tech Stack:** PostgreSQL 16, Spring Boot, Liquibase, spring.sql.init (ICM), preliquibase 1.5.1 (DMS, no change)

---

## File Map

### ICM (`intelligent_content_management`)

| Action | File |
|--------|------|
| Modify | `src/main/resources/docker/db-postgres/1_docker_postgres_initialisation.sh` |
| Modify | `src/main/resources/docker/docker-compose.yml` |
| Modify | `src/main/resources/application.yml` |
| Create | `src/main/resources/db/init/schema.sql` |
| Modify | `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/db.changelog-spring1-release1.xml` |
| Modify | `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-PRIVILEGES.sql` |
| Delete | `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql` |

### DMS (`dms`)

| Action | File |
|--------|------|
| Modify | `src/main/resources/application.yml` |
| Modify | `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/db.changelog.xml` |
| Delete | `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql` |

---

## Task 1: Drop Docker volumes for a clean start

Both services are changing databases/schemas. The Postgres init script only runs when the data directory is empty (first start on a blank volume), so existing volumes must be removed to allow `dms_db` to be created on next boot. This is a one-time migration step — after this, volumes persist normally across restarts.

**Files:** none

- [ ] **Step 1: Stop and remove containers and volumes**

```powershell
cd C:\Users\eastih\Documents\Home\Dev\workspaces\archetype_ddd\intelligent_content_management
docker compose -f src/main/resources/docker/docker-compose.yml down -v
```

Expected output: containers stopped, volumes `db`, `elasticsearch-data`, `pgadmin-data` removed.

- [ ] **Step 2: Verify volumes are gone**

```powershell
docker volume ls | Select-String "intelligent_content_management"
```

Expected: no output (volumes deleted).

---

## Task 2: Simplify the Postgres init shell script

The script's only job now is to create the `dms_db` database. `doc_management_db` is created by Postgres via `POSTGRES_DB`. No users, no schemas.

**Files:**
- Modify: `src/main/resources/docker/db-postgres/1_docker_postgres_initialisation.sh`

- [ ] **Step 1: Replace the script content**

Replace the entire file with:

```bash
#!/bin/bash

set -e
set -u

if [ -n "$POSTGRES_EXTRA_DATABASES" ]; then
  echo "Creating extra databases: $POSTGRES_EXTRA_DATABASES"
  for db in $(echo $POSTGRES_EXTRA_DATABASES | tr ',' ' '); do
    echo "  Creating database '$db'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
      -c "SELECT 1 FROM pg_database WHERE datname = '$db'" \
      | grep -q 1 || psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" \
      -c "CREATE DATABASE $db"
  done
  echo "Extra databases created"
fi
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/docker/db-postgres/1_docker_postgres_initialisation.sh
git commit -m "infra: simplify postgres init script — create databases only, no users or schemas"
```

---

## Task 3: Update ICM docker-compose

Replace `POSTGRES_MULTIPLE_DATABASES` (old format `db,user,schema`) with `POSTGRES_EXTRA_DATABASES` (new format: comma-separated DB names only).

**Files:**
- Modify: `src/main/resources/docker/docker-compose.yml`

- [ ] **Step 1: Update the environment block for the `db` service**

Find:
```yaml
    environment:
      - POSTGRES_MULTIPLE_DATABASES=doc_management_db,doc_management_user,documentcontent
      - POSTGRES_PASSWORD=toor
      - POSTGRES_DB=doc_management_db
      - POSTGRES_USER=postgres
```

Replace with:
```yaml
    environment:
      - POSTGRES_EXTRA_DATABASES=dms_db
      - POSTGRES_PASSWORD=toor
      - POSTGRES_DB=doc_management_db
      - POSTGRES_USER=postgres
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/docker/docker-compose.yml
git commit -m "infra: update docker-compose — replace POSTGRES_MULTIPLE_DATABASES with POSTGRES_EXTRA_DATABASES=dms_db"
```

---

## Task 4: Add ICM schema init SQL

ICM has no `preliquibase`. Use `spring.sql.init` to create the `documentcontent` schema before Liquibase runs.

**Files:**
- Create: `src/main/resources/db/init/schema.sql`

- [ ] **Step 1: Create the schema init file**

```sql
CREATE SCHEMA IF NOT EXISTS documentcontent;
```

Save to `src/main/resources/db/init/schema.sql`.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/db/init/schema.sql
git commit -m "infra: add spring.sql.init schema creation for documentcontent"
```

---

## Task 5: Update ICM application.yml

Add `currentSchema=documentcontent` to the JDBC URL and wire in `spring.sql.init`.

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Update datasource URL and add sql.init config**

Find:
```yaml
  datasource: #This is needed because docker compose is not able to detect the database if we are using custom docker images
    url: jdbc:postgresql://localhost:5434/doc_management_db
    username: postgres
    password: toor
```

Replace with:
```yaml
  datasource:
    url: jdbc:postgresql://localhost:5434/doc_management_db?currentSchema=documentcontent
    username: postgres
    password: toor
  sql:
    init:
      mode: always
      schema-locations: classpath:db/init/schema.sql
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.yml
git commit -m "config: set JDBC currentSchema for ICM, add spring.sql.init for documentcontent schema"
```

---

## Task 6: Remove ALTER ROLE changeSet and doc_management_user grants from ICM Liquibase

**Files:**
- Modify: `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/db.changelog-spring1-release1.xml`
- Modify: `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-PRIVILEGES.sql`
- Delete: `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql`

- [ ] **Step 1: Remove the `set_search_path` changeSet from the ICM changelog**

In `db.changelog-spring1-release1.xml`, remove this entire block:

```xml
    <changeSet id="set_search_path" author="eas" failOnError="true">
        <comment>set search path for postgres</comment>
        <sqlFile
                path="db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql"
                splitStatements="true"
                stripComments="true"
                encoding="UTF-8"
        />
    </changeSet>
```

Also add `<validCheckSum>ANY</validCheckSum>` to the `grant_all_privileges` changeSet (since its SQL will change in the next step):

```xml
    <changeSet id="grant_all_privileges" author="eas" failOnError="true">
        <validCheckSum>ANY</validCheckSum>
        <comment>grant all privileges to postgres</comment>
        <sqlFile
                path="db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-PRIVILEGES.sql"
                splitStatements="true"
                stripComments="true"
                encoding="UTF-8"
        />
    </changeSet>
```

- [ ] **Step 2: Update DATABASE-DDL-PRIVILEGES.sql — remove doc_management_user grants**

Replace the entire file content with:

```sql
-- Schema documentcontent
-- Schema privileges for postgres user
GRANT USAGE ON SCHEMA documentcontent TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA documentcontent TO postgres;
GRANT ALL ON ALL SEQUENCES IN SCHEMA documentcontent TO postgres;
GRANT ALL ON ALL FUNCTIONS IN SCHEMA documentcontent TO postgres;
```

- [ ] **Step 3: Delete DATABASE-DDL-SEARCH-PATH.sql**

```powershell
Remove-Item "src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql"
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/changelog/
git commit -m "db: remove ALTER ROLE search_path changeSet and doc_management_user grants from ICM Liquibase"
```

---

## Task 7: Update DMS application.yml

Point DMS at the new `dms_db` database with `currentSchema=vectorcontent`.

**Files:**
- Modify: `src/main/resources/application.yml` (in `dms` workspace)

- [ ] **Step 1: Update datasource URL**

Find:
```yaml
  datasource:
    url: jdbc:postgresql://localhost:5434/doc_management_db
    username: postgres
    password: toor
```

Replace with:
```yaml
  datasource:
    url: jdbc:postgresql://localhost:5434/dms_db?currentSchema=vectorcontent
    username: postgres
    password: toor
```

- [ ] **Step 2: Commit**

```bash
cd C:\Users\eastih\Documents\Home\Dev\workspaces\ai_chat_dms_workspace\dms
git add src/main/resources/application.yml
git commit -m "config: point DMS at dms_db with JDBC currentSchema=vectorcontent"
```

---

## Task 8: Remove ALTER ROLE changeSet from DMS Liquibase

**Files:**
- Modify: `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/db.changelog.xml` (DMS)
- Delete: `src/main/resources/db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql` (DMS)

- [ ] **Step 1: Remove the `set_search_path` changeSet from DMS changelog**

In DMS's `db.changelog.xml`, remove this entire block:

```xml
    <changeSet id="set_search_path" author="eas" failOnError="true">
        <comment>set search path for postgres</comment>
        <sqlFile
                path="db/changelog/DocumentContent-DOC_MANAGEMENT_DB/sprint1/release1/privileges/DATABASE-DDL-SEARCH-PATH.sql"
                splitStatements="true"
                stripComments="true"
                encoding="UTF-8"
        />
    </changeSet>
```

- [ ] **Step 2: Delete DMS DATABASE-DDL-SEARCH-PATH.sql**

```powershell
Remove-Item "C:\Users\eastih\Documents\Home\Dev\workspaces\ai_chat_dms_workspace\dms\src\main\resources\db\changelog\DocumentContent-DOC_MANAGEMENT_DB\sprint1\release1\privileges\DATABASE-DDL-SEARCH-PATH.sql"
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/changelog/
git commit -m "db: remove ALTER ROLE search_path changeSet from DMS Liquibase"
```

---

## Task 9: Smoke test — start both services

Verify the full stack starts clean with the new database layout.

- [ ] **Step 1: Start infrastructure (ICM triggers docker-compose)**

Start ICM — Spring Boot Docker Compose integration will spin up the container:

```powershell
cd C:\Users\eastih\Documents\Home\Dev\workspaces\archetype_ddd\intelligent_content_management
./mvnw spring-boot:run
```

Expected: Postgres starts, `doc_management_db` and `dms_db` are created, `documentcontent` schema is created via `spring.sql.init`, ICM Liquibase migrations complete, application starts on port 8085.

- [ ] **Step 2: Verify both databases exist**

```powershell
docker exec spring-postgresql-db psql -U postgres -c "\l"
```

Expected: both `doc_management_db` and `dms_db` appear in the list.

- [ ] **Step 3: Verify documentcontent schema exists in doc_management_db**

```powershell
docker exec spring-postgresql-db psql -U postgres -d doc_management_db -c "\dn"
```

Expected: `documentcontent` schema listed.

- [ ] **Step 4: Start DMS**

```powershell
cd C:\Users\eastih\Documents\Home\Dev\workspaces\ai_chat_dms_workspace\dms
./mvnw spring-boot:run
```

Expected: DMS Liquibase creates `vectorcontent` schema in `dms_db` (via preliquibase), vector_store table created, application starts on port 8086.

- [ ] **Step 5: Verify vectorcontent schema exists in dms_db**

```powershell
docker exec spring-postgresql-db psql -U postgres -d dms_db -c "\dn"
```

Expected: `vectorcontent` schema listed.

- [ ] **Step 6: Verify doc_management_user does not exist**

```powershell
docker exec spring-postgresql-db psql -U postgres -c "\du"
```

Expected: only `postgres` role listed, no `doc_management_user`.
