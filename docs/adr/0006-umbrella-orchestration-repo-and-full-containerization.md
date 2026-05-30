# Umbrella orchestration repo with full containerization

To run the whole system with one command, all three applications (ICM, DMS, frontend) plus their dependencies are containerized and started by a single global `docker-compose.yml`. The DMS repo is relocated to sit beside ICM under `archetype_ddd/`, and `archetype_ddd/` itself becomes a thin **umbrella git repo** that owns the compose file and orchestration config. ICM and DMS remain independent nested repos.

## Considered Options

- **Umbrella repo at `archetype_ddd` (chosen)** — orchestration is a system-of-systems concern living above both bounded contexts; all build contexts become clean relative paths down into each repo.
- **Compose inside the ICM repo, DMS via `../dms`** — ICM is the runtime composition root, but a child repo reaching up to build a sibling is a layering inversion: `docker compose build` reads files outside ICM's boundary and a lone ICM clone has a dangling DMS context.
- **Dedicated deploy repo** — clean separation but a third repo to clone/maintain.

## Consequences

- The apps stay topology-agnostic: all service-name wiring (DB/ES/Keycloak/DMS/Ollama hosts) and `SPRING_DOCKER_COMPOSE_ENABLED=false` are injected as **env vars from compose**, not committed into the app repos.
- ICM's Spring Boot Docker Compose self-launch must be disabled when containerized (no docker socket in-container).
- Dockerfiles must be authored for ICM, DMS, and the frontend (none exist today); images rebuild per development iteration.
- Ollama runs as a container with a persistent model volume and a one-time model-pull init (CPU-only unless NVIDIA+WSL2 GPU is configured).
- Keycloak is pinned (`KC_HOSTNAME`) to issue a stable `localhost:8180` issuer; ICM splits `issuer-uri` (public, for `iss` matching) from `jwk-set-uri` (internal `keycloak:8080`, for key fetch) so it boots in-container and still validates browser-issued tokens.
- `CLAUDE.md`'s `/add-dir` path must be updated to the new DMS location after the move.
