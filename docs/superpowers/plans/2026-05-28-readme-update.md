# README Update — ICM + DMS Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the stale ICM README and create a DMS README from scratch, both architecture-first and cross-linked.

**Architecture:** Each README opens with a shared system diagram, followed by bounded-context description, ADR summaries, tech stack table, key commands, and API info. The two READMEs are system-aware — each links to the other using full GitHub URLs.

**Tech Stack:** Markdown only. No tooling beyond a text editor and `git`.

**Repos:**
- ICM: `C:\Users\eastih\Documents\Home\Dev\workspaces\archetype_ddd\intelligent_content_management` → `https://github.com/emileastih1/Intelligent_Content_Management`
- DMS: `C:\Users\eastih\Documents\Home\Dev\workspaces\ai_chat_dms_workspace\dms` → `https://github.com/emileastih1/SpringAiServiceClient`

**Spec:** `docs/superpowers/specs/2026-05-28-readme-design.md`

---

## Files

| Action | Path |
|---|---|
| Modify | `intelligent_content_management/README.md` |
| Create | `dms/README.md` |

---

### Task 1: Rewrite ICM README.md

**Files:**
- Modify: `intelligent_content_management/README.md` (full rewrite)

- [ ] **Step 1: Write the new README**

Replace the entire content of `intelligent_content_management/README.md` with:

````markdown
# intelligent-content-management

> Document storage, full-text search, and API gateway for the Intelligent Document Management system.

## System Overview

```
         API clients (curl / Postman / Swagger UI)
               │  Bearer JWT
               ▼
 ┌──────────────────────────┐   HTTP   ┌──────────────────────────┐
 │  intelligent-content-    │ ───────▶ │          dms             │
 │   management (this svc)  │          │   (AI / RAG service)     │
 │  port 8085  /idm         │          │   port 8086              │
 └───┬─────────┬─────┬──────┘          └───────┬──────────────────┘
     │         │     │                         │
     ▼         ▼     ▼                         ▼
 PostgreSQL  Elastic Keycloak       PostgreSQL + pgvector
  (docs)    (search)  (IdP)          (vector store)
                                   + Ollama (local LLM)
```

This service owns the document lifecycle: storage, metadata, full-text indexing, and the secured REST API. AI/RAG operations are delegated over HTTP to the [dms](https://github.com/emileastih1/SpringAiServiceClient) microservice.

## What This Service Does

Persists document metadata and binary content to PostgreSQL, extracts text with Apache Tika, and indexes it in Elasticsearch for full-text search. Exposes a JWT-secured REST API — all endpoints require a Bearer token issued by Keycloak. AI capabilities (question-answering, embeddings) are handled by the `dms` microservice; this service holds only the outbound HTTP port that calls it.

## Architecture Decisions

**ADR 0001 — AI capability extracted to `dms`**
Early versions included Spring AI and OpenAI configuration. As RAG requirements grew (vector storage, embedding models, local Ollama), mixing AI inference with document management conflated two distinct concerns. All AI logic was moved to the `dms` microservice. This service retains no Spring AI dependency — it holds only the `AiServiceClient` outbound HTTP port. See [`docs/adr/0001-ai-capability-extracted-to-dms-microservice.md`](docs/adr/0001-ai-capability-extracted-to-dms-microservice.md).

**ADR 0002 — Keycloak as identity provider**
Replaced in-memory Basic Auth with Keycloak running as a Docker Compose service. The realm configuration is version-controlled as `realm-export.json`, making setup reproducible with `docker compose up`. Client roles `READ` and `WRITE` are enforced at the method level via `@PreAuthorize`. Keycloak was preferred over Auth0/Okta (no vendor lock-in, no external runtime dependency) and over Spring Authorization Server (embedding an auth server in the resource server process conflates two responsibilities). See [`docs/adr/0002-keycloak-as-identity-provider.md`](docs/adr/0002-keycloak-as-identity-provider.md).

## Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.5 | Application framework |
| Spring Security OAuth2 Resource Server | — | JWT validation |
| Keycloak | 26 | OAuth2/OIDC identity provider |
| PostgreSQL | 16 | Document metadata persistence |
| Elasticsearch | 8.12 | Full-text document search |
| Apache Tika | — | Document content extraction |
| Liquibase | — | Database schema migrations |
| Testcontainers | — | Integration test infrastructure |
| Docker Compose | — | Local service orchestration |

## Getting Started

**Prerequisites:** Java 21, Docker Desktop running.

```bash
# 1. Start backing services (PostgreSQL, Elasticsearch, Keycloak)
docker compose -f src/main/resources/docker/docker-compose.yml up -d

# 2. Run the application
./mvnw spring-boot:run

# 3. Explore the API
# http://localhost:8085/idm/swagger-ui/index.html
```

**Obtain a Bearer token** before calling secured endpoints:

```bash
curl -s -X POST \
  http://localhost:8180/realms/intelligent-content-management/protocol/openid-connect/token \
  -d "grant_type=password&client_id=intelligent-content-management-api&client_secret=icm-secret&username=user-read&password=password" \
  | jq -r .access_token
```

Test users: `user-read` (READ role), `user-write` (WRITE role). Password: `password`.

AI endpoints (e.g. `POST /api/v1/document/ask`) require the `dms` service — see the [dms README](https://github.com/emileastih1/SpringAiServiceClient).

## API

| | |
|---|---|
| Swagger UI | http://localhost:8085/idm/swagger-ui/index.html |
| Context path | `/idm` |
| Auth | Bearer JWT — obtain from Keycloak (see Getting Started) |

## Running Tests

```bash
# Full suite — unit + integration (Testcontainers: PostgreSQL, Elasticsearch, Keycloak)
./mvnw test -Pwindows-docker-desktop
```

The `windows-docker-desktop` Maven profile sets `DOCKER_HOST` to the Docker Desktop named pipe so Testcontainers can reach the Docker socket.

**Elasticsearch on Windows/WSL2** — run once after Docker Desktop starts:

```bash
wsl -d docker-desktop
sysctl -w vm.max_map_count=262144
```

## Support

[Open an issue](https://github.com/emileastih1/Intelligent_Content_Management/issues) · emileastih1@gmail.com
````

- [ ] **Step 2: Verify the file looks right**

Read `README.md` and check:
- System diagram renders with correct box labels (`this svc` on left box)
- Both ADR links use relative paths that resolve within the repo
- Keycloak token curl command has correct realm name (`intelligent-content-management`) and client ID (`intelligent-content-management-api`)
- DMS link points to `https://github.com/emileastih1/SpringAiServiceClient`

- [ ] **Step 3: Commit**

```bash
cd intelligent_content_management
git add README.md
git commit -m "docs: rewrite README — architecture-first, JWT security, remove stale Spring AI content"
```

---

### Task 2: Create DMS README.md

**Files:**
- Create: `dms/README.md`

- [ ] **Step 1: Write the DMS README**

Create `dms/README.md` with the following content:

````markdown
# dms

> AI / RAG microservice for the Intelligent Document Management system.

## System Overview

```
         API clients (curl / Postman / Swagger UI)
               │  Bearer JWT
               ▼
 ┌──────────────────────────┐   HTTP   ┌──────────────────────────┐
 │  intelligent-content-    │ ───────▶ │          dms             │
 │      management          │          │  (AI / RAG — this svc)   │
 │  port 8085  /idm         │          │   port 8086              │
 └───┬─────────┬─────┬──────┘          └───────┬──────────────────┘
     │         │     │                         │
     ▼         ▼     ▼                         ▼
 PostgreSQL  Elastic Keycloak       PostgreSQL + pgvector
  (docs)    (search)  (IdP)          (vector store)
                                   + Ollama (local LLM)
```

This service owns document ingestion and question-answering. It is called over HTTP by [intelligent-content-management](https://github.com/emileastih1/Intelligent_Content_Management); API clients never call it directly.

## What This Service Does

Ingests documents by generating vector embeddings with `mxbai-embed-large` and storing them in a pgvector-backed PostgreSQL table. Answers natural-language queries via a RAG pipeline: similarity search retrieves relevant document chunks, which are passed as context to `gemma3:4b` running locally in Ollama. Both models run entirely on-device — no external AI API calls.

## Architecture Decisions

**ADR 0001 — pgvector extension installed in `public` schema**
The `vector`, `hstore`, and `uuid-ossp` extensions must be installed in `public`, not in the `vectorcontent` application schema. Spring AI's `PgVectorStore` does not schema-qualify the `vector` type; if the extension lives in a non-default schema, every connection that lacks `vectorcontent` in its `search_path` fails with `PSQLException: Unknown type vector`. The application tables remain in `vectorcontent`. See [`docs/adr/0001-pgvector-extension-in-public-schema.md`](docs/adr/0001-pgvector-extension-in-public-schema.md).

**ADR 0002 — Cosine similarity as the vector distance metric**
The HNSW index on `vector_store` uses `vector_cosine_ops` to match `mxbai-embed-large`, which produces normalised vectors. The operator class is fixed at index creation time — swapping the embedding model requires a Liquibase migration that drops and recreates the index with the correct operator class. See [`docs/adr/0002-cosine-similarity-for-vector-store-index.md`](docs/adr/0002-cosine-similarity-for-vector-store-index.md).

**ADR 0003 — `jsonb` for the `metadata` column**
Spring AI metadata filters use the `@>` (contains) operator, which is only available on `jsonb`, not `json`. Defining `metadata` as `json` silently prevented all metadata-filtered similarity searches. See [`docs/adr/0003-metadata-column-jsonb.md`](docs/adr/0003-metadata-column-jsonb.md).

**ADR 0004 — Modify existing changesets pre-production**
Foundational mistakes in `release1` and `release2` (wrong extension schema, wrong metadata column type) were corrected in-place rather than via an additive `release3` changeset. This is valid because the project has no production database — all dev databases are ephemeral Testcontainers instances. If your local DB predates this change, run `./mvnw liquibase:clearCheckSums` before the next `./mvnw spring-boot:run`. See [`docs/adr/0004-modify-existing-changesets-pre-production.md`](docs/adr/0004-modify-existing-changesets-pre-production.md).

## Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 4.0.5 | Application framework |
| Spring AI | 2.0.0-M7 | RAG pipeline orchestration |
| Ollama | local | LLM runtime |
| gemma3:4b | — | Chat / generation model |
| mxbai-embed-large | — | Embedding model (1024 dims, cosine) |
| PostgreSQL + pgvector | 16 | Vector store (`vectorcontent` schema) |
| Liquibase | — | Schema migrations |
| Testcontainers | 1.21.0 | Integration test infrastructure |
| Docker Compose | — | Local PostgreSQL orchestration |

## Getting Started

**Prerequisites:** Java 21, Docker Desktop, [Ollama](https://ollama.com) installed and running.

```bash
# 1. Pull the required models (one-time, ~2–5 GB each)
ollama pull gemma3:4b
ollama pull mxbai-embed-large

# 2. Start PostgreSQL
docker compose up -d

# 3. Run the application
./mvnw spring-boot:run

# 4. Explore the API
# http://localhost:8086/AiServiceClient/swagger-ui.html
```

This service is a backend dependency of `intelligent-content-management`. See the [intelligent-content-management README](https://github.com/emileastih1/Intelligent_Content_Management) for the full system setup.

## API

| | |
|---|---|
| Swagger UI | http://localhost:8086/AiServiceClient/swagger-ui.html |
| Context path | `/AiServiceClient` |
| Auth | None — called service-to-service from `intelligent-content-management` |

## Running Tests

```bash
./mvnw test -Pwindows-docker-desktop
```

**Docker Desktop 29.x** — `~/.testcontainers.properties` must contain:

```
docker.host=npipe:////./pipe/docker_cli
```

Testcontainers is pinned at **1.21.0** for Docker Desktop 29.x compatibility (`src/test/resources/docker-java.properties` sets `api.version=1.44` — do not remove this file).

## Support

[Open an issue](https://github.com/emileastih1/Intelligent_Content_Management/issues) · emileastih1@gmail.com
````

- [ ] **Step 2: Verify the file looks right**

Read `dms/README.md` and check:
- System diagram has `this svc` label on the right (DMS) box
- All four ADR links use relative paths that resolve within the repo
- ICM link points to `https://github.com/emileastih1/Intelligent_Content_Management`
- Testcontainers version matches `pom.xml` (`1.21.0`)
- `docker.host` pipe name matches `~/.testcontainers.properties` (`docker_cli`)

- [ ] **Step 3: Commit**

```bash
cd dms
git add README.md
git commit -m "docs: add README — architecture-first, RAG stack, ADR summaries, cross-link to ICM"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** Goals → both READMEs covered. System diagram ✓. ADR summaries ✓ (2 in ICM, 4 in DMS). Tech stack tables ✓. Getting Started with key commands ✓. API section ✓. Cross-links ✓ (full GitHub URLs, both directions). Stale content removed (Spring AI/OpenAI config, pgAdmin screenshot, old branch notes) ✓.
- [x] **Placeholders:** None. All URLs, commands, port numbers, realm names, and model names are filled in with actual values from the codebase.
- [x] **Consistency:** Port 8085 / `/idm` for ICM ✓. Port 8086 / `/AiServiceClient` for DMS ✓. Keycloak realm `intelligent-content-management` ✓. Client ID `intelligent-content-management-api` ✓. DMS GitHub URL `SpringAiServiceClient` ✓.
