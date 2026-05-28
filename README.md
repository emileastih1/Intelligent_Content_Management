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
