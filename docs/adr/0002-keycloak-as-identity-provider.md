# ADR 0002: Keycloak as identity provider for OAuth2/JWT security

## Status

Accepted

## Context

The service needed to replace its in-memory Basic Auth placeholder with a production-grade identity solution. The main candidates were:

- **Keycloak** — open-source, self-hosted, full OAuth2/OIDC provider
- **Auth0 / Okta** — managed cloud IdP services
- **Spring Authorization Server** — Spring-native, embedded in the same process
- **Continued Basic Auth** — simplest, but not viable for production

## Decision

Use Keycloak as the identity provider, running as a Docker Compose service alongside PostgreSQL and Elasticsearch.

Reasons:

1. **Self-hosted, no vendor lock-in** — Auth0/Okta add a runtime SaaS dependency and cost at scale. Keycloak runs locally and in any environment without external calls.
2. **Full OAuth2/OIDC out of the box** — realm, client, role, and user management without writing any auth server code.
3. **Realistic local dev** — the goal of this archetype is to demonstrate production patterns. A managed cloud IdP hides the identity configuration; Keycloak makes it explicit and inspectable.
4. **Spring Authorization Server rejected** — embedding the auth server in the same process as the resource server conflates two distinct responsibilities. It would also need to be extracted later if a second service (`dms`) joins the same realm.

The realm is named `intelligent-content-management`. This service is registered as a confidential client (`intelligent-content-management-api`) with two client roles: `READ` and `WRITE`.

## Consequences

- Keycloak is a required runtime dependency for any secured environment. Local dev requires `docker compose up`.
- The realm configuration is version-controlled as a Keycloak realm export JSON (`src/main/resources/docker/keycloak/realm-export.json`), making setup reproducible with a single command.
- If a managed IdP is required in future (e.g., enterprise deployment), Spring Security's OAuth2 Resource Server abstraction means only the `issuer-uri` property changes — no application code changes needed.
