# PRD: Global compose & containerization (umbrella repo, all three apps + infra)

> Depends on the Frontend → ICM wiring PRD (#68): the BFF and the grown ICM/DMS contracts should exist before the stack is containerized. Implements ADR-0006 (umbrella orchestration repo + full containerization). Respects ADR-0005 (BFF service-account auth) for the Keycloak client wiring.

## Problem Statement

Running the system today is a manual, multi-terminal chore. ICM auto-launches an infra-only compose (Postgres, Elasticsearch, Keycloak, pgAdmin) via Spring Boot's Docker Compose integration, but the three applications (ICM, DMS, frontend) are each started by hand, Ollama must already be running on the host, and the two service repos live in unrelated parent folders. There is no single way to bring the whole system up. A developer (or a new teammate) cannot run one command and get a working stack.

## Solution

A single global `docker-compose.yml` brings up everything — all three applications plus every dependency they need (PostgreSQL/pgvector, Elasticsearch, Keycloak, Ollama, pgAdmin) — with one command. The applications run as containers built from their own Dockerfiles, rebuilt per iteration. The DMS repo is relocated to sit beside ICM under `archetype_ddd/`, which becomes a thin umbrella git repo owning the compose file and orchestration config (ADR-0006). The applications stay topology-agnostic: all host/URL wiring is injected as environment variables from the compose, and ICM's self-launched infra compose is disabled when containerized. A cold `docker compose up` yields a fully wired, verifiable system.

## User Stories

1. As a developer, I want to run one command and have ICM, DMS, and the frontend all start, so that I don't juggle multiple terminals.
2. As a developer, I want the same command to start every dependency (Postgres, Elasticsearch, Keycloak, Ollama, pgAdmin), so that nothing has to be pre-installed or pre-running on my host.
3. As a developer, I want each application built from its own Dockerfile, so that the container always reflects my current code.
4. As a developer, I want images rebuilt on each iteration, so that my changes are picked up when I bring the stack up.
5. As a developer, I want the DMS repo to live beside ICM under `archetype_ddd/`, so that all build contexts are clean relative paths.
6. As a developer, I want `archetype_ddd/` to be an umbrella git repo owning the compose and orchestration config, so that the deploy topology is version-controlled above both bounded contexts.
7. As a developer, I want ICM and DMS to remain independent nested repos, so that each bounded context keeps its own history.
8. As a developer, I want the applications to receive their service hosts via environment variables, so that they stay topology-agnostic and no docker awareness leaks into the app repos.
9. As a developer, I want ICM's Spring Boot Docker Compose self-launch disabled in-container, so that ICM boots without a docker socket.
10. As a developer, I want Ollama to run as a container with its models persisted in a volume, so that the AI works without a host Ollama and models are pulled only once.
11. As a developer, I want the required models (gemma3:4b, mxbai-embed-large) pulled automatically on first run, so that DMS has what it needs without manual setup.
12. As a developer, I want Keycloak pinned to a stable issuer (`localhost:8180`) and ICM to fetch keys internally (`keycloak:8080`), so that ICM boots in-container and can still validate browser-issued tokens.
13. As a developer, I want a confidential Keycloak client for the BFF (service account, `READ`+`WRITE`), so that the BFF can authenticate to ICM (ADR-0005).
14. As a developer, I want the database to come up with both schemas/databases (doc_management_db, dms_db) initialised, so that ICM and DMS each have their store.
15. As a developer, I want containers to start in the right order with healthchecks, so that apps don't start before their dependencies are ready.
16. As a developer, I want DMS to wait until Ollama's models are present, so that the first questions don't fail.
17. As a developer, I want to reach the frontend in my browser on a known port, so that I can use the app.
18. As a developer, I want ICM and DMS Swagger UIs reachable, so that I can inspect and exercise the APIs.
19. As a developer, I want pgAdmin available, so that I can inspect the database.
20. As a developer, I want a scripted smoke verification that brings the stack up cold and asserts every service is healthy, so that wiring regressions are caught automatically.
21. As a developer, I want the smoke check to confirm Ollama has both models, so that the AI path is actually ready, not just running.
22. As a developer, I want persistent volumes for database, Elasticsearch, pgAdmin, and Ollama models, so that data survives restarts.
23. As a developer, I want an `.env` / `.env.example` for secrets and tunables, so that I can configure the stack without editing the compose file.
24. As a developer, I want the `CLAUDE.md` `/add-dir` path updated after the DMS move, so that agent sessions still load both projects.
25. As a developer, I want to tear the stack down and bring it back cleanly, so that I can reset state when needed.
26. As a developer with an NVIDIA+WSL2 GPU, I want documented (optional) GPU enablement for Ollama, so that I can accelerate it if my machine supports it.

## Implementation Decisions

### Repository topology (ADR-0006)
- The DMS repo is **relocated** to `archetype_ddd/dms`, beside `archetype_ddd/intelligent_content_management` (which already contains the frontend at `frontend/`).
- `archetype_ddd/` becomes a thin **umbrella git repo** owning the global `docker-compose.yml`, `.env`/`.env.example`, and the relocated infra support files. Its `.gitignore` excludes the nested `intelligent_content_management/` and `dms/` repos.
- Build contexts are relative: `./intelligent_content_management`, `./intelligent_content_management/frontend`, `./dms`.
- `CLAUDE.md`'s `/add-dir` instruction is updated to the new DMS path.

### Dockerfiles
- **ICM** and **DMS**: multi-stage — a Maven build stage (Java 21, using the project `mvnw`) producing the boot jar, then a slim JRE 21 runtime stage running the jar. Each repo gets a `.dockerignore`.
- **Frontend**: multi-stage — a Node build stage running `npm ci` + `npm run build` (vite build + esbuild → `dist/server.cjs`), then a Node runtime stage running `node dist/server.cjs` with `NODE_ENV=production`, listening on `:3000`.

### Global compose services
- `db` — pgvector/pg16; initialises `doc_management_db` (schema `documentcontent`) and `dms_db` (schema `vectorcontent`) via the existing multi-database init script; published on `5434`.
- `pgadmin`, `elasticsearch` (single-node, security disabled), `keycloak` (`start-dev --import-realm`, realm imported from the relocated `realm-export.json`).
- `ollama` — `ollama/ollama` with a named volume for models; `ollama-init` — a one-shot service that pulls `gemma3:4b` and `mxbai-embed-large` into that volume on first run.
- `dms`, `icm`, `frontend` — built from the Dockerfiles above.
- One bridge network; named volumes for db, elasticsearch, pgadmin, and ollama models.

### Topology wiring (env vars from compose, ADR-0006)
- ICM: `SPRING_DATASOURCE_URL` → `database:5432`, `SPRING_ELASTICSEARCH_URIS` → `elasticsearch:9200`, `AISERVICECLIENT_URL` → `dms:8086`, `SPRING_DOCKER_COMPOSE_ENABLED=false`.
- DMS: `SPRING_DATASOURCE_URL` → `database:5432`, Ollama base URL → `ollama:11434`.
- Frontend BFF: ICM base URL → `icm:8085/idm`; Keycloak token endpoint + service-account client id/secret (ADR-0005).
- No localhost URLs remain hardcoded for in-container runtime; the app repos are unchanged beyond reading these env vars.

### Keycloak issuer (ADR-0006)
- Keycloak pinned with `KC_HOSTNAME=localhost`, `KC_HOSTNAME_PORT=8180`, `KC_HOSTNAME_STRICT=false`, `KC_HTTP_ENABLED=true` so it always issues `iss = http://localhost:8180/realms/intelligent-content-management`.
- ICM gets `...jwt.issuer-uri = http://localhost:8180/...` (literal `iss` match, no network call) and `...jwt.jwk-set-uri = http://keycloak:8080/.../certs` (internal key fetch). This makes ICM boot in-container and still validate browser-issued tokens.
- The realm defines a confidential client for the BFF with `READ`+`WRITE` roles (client-credentials).

### Startup ordering & healthchecks
- Healthchecks on `db`, `elasticsearch`, `keycloak`, `ollama`. `depends_on` with `condition: service_healthy` so apps wait for their dependencies.
- DMS additionally waits for `ollama-init` to complete (models present) before it is considered ready.

### Smoke verification (chosen)
- A scripted smoke check that: brings the stack up from cold, waits for all healthchecks, then probes each service — ICM (`/idm` actuator/Swagger), DMS (`/AiServiceClient` Swagger), frontend (`:3000`), Keycloak (realm OIDC metadata), Ollama (both models present) — and exits non-zero if any is not green.

## Testing Decisions

A good test here asserts **observable system behavior** — "the stack comes up and every service is reachable/ready" — not the internals of any container. It must run from a cold state (no pre-existing volumes/containers), be repeatable, and fail loudly when wiring breaks.

- **Scripted smoke verification (in scope):** the cold-start smoke check described above is the single automated verification. It is the prior-art-free equivalent of an integration test for the orchestration layer; it complements (does not replace) the in-container correctness already covered by each app's own test suite.
- No unit tests are specified for Dockerfiles or compose config (no isolatable logic). Per-app unit/integration suites (e.g. ICM `DocumentRagIntegrationTest`, DMS `DocumentAiRepositoryIT`) continue to run inside their own builds and are unaffected.

## Out of Scope

- **Production hardening** — TLS, secrets management beyond `.env`, non-dev Keycloak (`start-dev` is retained), resource tuning, multi-host orchestration (Kubernetes).
- **GPU as default** — containerized Ollama runs CPU-only by default; NVIDIA+WSL2 GPU enablement is documented as optional, not wired on by default.
- **Hot reload in containers** — images rebuild per iteration; live code reload is not provided.
- **CI/CD pipelines** — building/publishing images in CI is not covered.
- **Application behavior changes** — all functional behavior is delivered by the Frontend → ICM wiring PRD (#68); this PRD only containerizes and orchestrates.
- **Persisting the umbrella repo to a remote** — `git init` locally is in scope; choosing/pushing to a remote is not.

## Further Notes

- Sequence after #68: the frontend BFF must already authenticate via the service account and the grown ICM/DMS contracts must exist, or the containerized stack will come up but the app paths won't work end-to-end.
- The existing infra-only compose under ICM (`src/main/resources/docker/`) is effectively superseded for full-stack runs; ICM's Spring Docker Compose integration is disabled in-container via env var but may remain for pure local-IDE development outside the global compose.
- First `docker compose up` is slow (Maven builds for two apps + multi-GB Ollama model pull); subsequent runs reuse layers and the model volume.
- The Keycloak realm export must include the BFF confidential client and the `READ`/`WRITE` roles; if it doesn't yet, that realm change rides along with this work.
