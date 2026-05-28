# Spec: README update — ICM + DMS

**Date:** 2026-05-28  
**Scope:** `intelligent-content-management/README.md` and `dms/README.md`

---

## Goals

- Replace the stale ICM README (still references Spring AI / OpenAI, old test commands, wrong branch notes)
- Create a DMS README from scratch (none exists)
- Both READMEs serve dual audience: developer setup guide + architecture portfolio
- Architecture-first (Option A): system diagram is the first thing a reader sees
- System-aware: each README links to the other and shows the full two-service picture
- Key commands only in Getting Started — assume Java/Docker competence

---

## System diagram (shared across both READMEs, perspective note varies)

```
         API clients (curl / Postman / Swagger UI)
               │  Bearer JWT
               ▼
 ┌──────────────────────────┐   HTTP   ┌──────────────────────────┐
 │  intelligent-content-    │ ───────▶ │          dms             │
 │      management          │          │   (AI / RAG service)     │
 │  port 8085  /idm         │          │   port 8086              │
 └───┬─────────┬─────┬──────┘          └───────┬──────────────────┘
     │         │     │                         │
     ▼         ▼     ▼                         ▼
 PostgreSQL  Elastic Keycloak       PostgreSQL + pgvector
  (docs)    (search)  (IdP)          (vector store)
                                   + Ollama (local LLM)
```

ICM's diagram labels "this service" on the left box.  
DMS's diagram labels "this service" on the right box.

---

## ICM README content plan

### Header
```
# intelligent-content-management
> Document storage, full-text search, and API gateway for the Intelligent Document Management system.
```

### System Overview
The diagram above. One sentence below: "This service owns document lifecycle. AI/RAG operations are delegated to [dms](link)."

### What This Service Does
3 sentences covering: document CRUD + metadata (PostgreSQL), full-text indexing (Elasticsearch + Tika extraction), JWT-secured REST API (Keycloak). Does not run AI — delegates to `dms` over HTTP.

### Architecture Decisions
Two ADRs summarised (1 paragraph each):
- **ADR 0001 — AI extracted to `dms`**: Spring AI and OpenAI config removed. AI lives in `dms`; ICM holds only the outbound HTTP port.
- **ADR 0002 — Keycloak as identity provider**: Self-hosted OAuth2/OIDC via Docker Compose. Realm config version-controlled as `realm-export.json`. Client roles: `READ` / `WRITE`. Chose over Auth0/Okta (no vendor lock-in) and Spring Authorization Server (conflates resource server + auth server).

Link to `docs/adr/` for full text.

### Tech Stack (table)

| Technology | Version | Role |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.5 | Application framework |
| spring-boot-starter-oauth2-resource-server | — | JWT validation |
| Keycloak | 26 | OAuth2/OIDC identity provider |
| PostgreSQL | 16 | Document metadata persistence |
| Elasticsearch | 8.12 | Full-text document search |
| Apache Tika | — | Document content extraction |
| Liquibase | — | Database schema migrations |
| Testcontainers | — | Integration test infrastructure |
| Docker Compose | — | Local service orchestration |

### Getting Started

Prerequisites: Java 21, Docker Desktop running.

```bash
# 1. Start backing services (PostgreSQL, Elasticsearch, Keycloak)
docker compose -f src/main/resources/docker/docker-compose.yml up -d

# 2. Run the application
./mvnw spring-boot:run

# 3. Explore the API
open http://localhost:8085/idm/swagger-ui/index.html
```

Obtain a Bearer token from Keycloak before calling secured endpoints:
```bash
curl -s -X POST http://localhost:8180/realms/intelligent-content-management/protocol/openid-connect/token \
  -d "grant_type=password&client_id=intelligent-content-management-api&client_secret=icm-secret&username=user-read&password=password" \
  | jq -r .access_token
```
Test users: `user-read` (READ role), `user-write` (WRITE role). Password: `password`.

AI endpoints (`POST /api/v1/document/ask`) require the `dms` service to be running — see [dms README](link).

### Running Tests

```bash
# Unit + integration (Testcontainers: PostgreSQL, Elasticsearch, Keycloak)
./mvnw test -Pwindows-docker-desktop
```

Windows note: the `windows-docker-desktop` Maven profile sets `DOCKER_HOST` to the Docker Desktop named pipe for Testcontainers.

### Windows / WSL2 — Elasticsearch

```bash
wsl -d docker-desktop
sysctl -w vm.max_map_count=262144
```

### API

- Swagger UI: `http://localhost:8085/idm/swagger-ui/index.html`
- Context path: `/idm`
- Auth: Bearer JWT (obtain from Keycloak — see Getting Started)

### Support

Issue tracker: https://github.com/emileastih1/Intelligent_Content_Management/issues

---

## DMS README content plan

### Header
```
# dms
> AI / RAG microservice for the Intelligent Document Management system.
```

### System Overview
Same diagram. One sentence: "This service owns document ingestion and question-answering. It is called over HTTP by [intelligent-content-management](link)."

### What This Service Does
3 sentences: ingests documents (generates embeddings with `mxbai-embed-large`, stores in pgvector), answers natural-language queries via RAG (similarity search → `gemma3:4b` via Ollama), exposes a REST API consumed by `intelligent-content-management`.

### Architecture Decisions
Four ADRs summarised (1 paragraph each):
- **ADR 0001 — pgvector extension in `public` schema**: Extensions (`vector`, `hstore`, `uuid-ossp`) must be installed in `public`, not `vectorcontent`. Spring AI does not schema-qualify the `vector` type; installing in a non-default schema causes `PSQLException: Unknown type vector`.
- **ADR 0002 — Cosine similarity**: HNSW index uses `vector_cosine_ops` to match `mxbai-embed-large`'s normalised output. Changing the embedding model requires a migration that drops and recreates the index.
- **ADR 0003 — `jsonb` for metadata column**: Spring AI metadata filters use the `@>` operator, which is only available on `jsonb`, not `json`.
- **ADR 0004 — Modify changesets pre-production**: Foundational mistakes in `release1`/`release2` were corrected in-place (not via `release3`) because the project has no production database. Run `./mvnw liquibase:clearCheckSums` if your local DB predates this change.

Link to `docs/adr/` for full text.

### Tech Stack (table)

| Technology | Version | Role |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.5 | Application framework |
| Spring AI | 2.0.0-M7 | RAG pipeline orchestration |
| Ollama | local | LLM runtime |
| gemma3:4b | — | Chat / generation model |
| mxbai-embed-large | — | Embedding model (1024 dims) |
| PostgreSQL + pgvector | 16 | Vector store (`vectorcontent` schema) |
| Liquibase | — | Schema migrations |
| Testcontainers | 1.21.0 | Integration test infrastructure |
| Docker Compose | — | Local PostgreSQL orchestration |

### Getting Started

Prerequisites: Java 21, Docker Desktop, [Ollama](https://ollama.com) installed.

```bash
# 1. Pull the required models (one-time)
ollama pull gemma3:4b
ollama pull mxbai-embed-large

# 2. Start PostgreSQL
docker compose up -d

# 3. Run the application
./mvnw spring-boot:run

# 4. Explore the API
open http://localhost:8086/AiServiceClient/swagger-ui.html
```

This service is a backend dependency of `intelligent-content-management`. You do not normally call it directly in production — see [intelligent-content-management README](link).

### Running Tests

```bash
./mvnw test -Pwindows-docker-desktop
```

Testcontainers is pinned at **1.21.0** for Docker Desktop 29.x compatibility. `~/.testcontainers.properties` must contain:
```
docker.host=npipe:////./pipe/docker_cli
```

### API

- Swagger UI: `http://localhost:8086/AiServiceClient/swagger-ui.html`
- Context path: `/AiServiceClient`
- Auth: none (called service-to-service from `intelligent-content-management`)

### Support

Issue tracker: https://github.com/emileastih1/Intelligent_Content_Management/issues

---

## Out of scope

- Dependency audit section (already well-documented in ICM README; leave as-is or trim to a one-liner)
- pgAdmin config screenshot / `img.png` reference (remove from ICM — outdated)
- Branch-specific notes (`feat/spring-boot-4-upgrade` branch mention in ICM README — remove, that work is merged)
- Ollama model-swap instructions (DMS CLAUDE.md covers this; README stays at "pull these two models")
