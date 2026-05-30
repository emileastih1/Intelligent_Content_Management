# Express BFF fronts ICM, authenticated by a service account

The frontend keeps its existing Express server as a **Backend-For-Frontend**: the browser calls same-origin `/api/*`, Express proxies to ICM, and Express relays ICM's SSE `/ask` stream to the browser. The BFF authenticates to ICM with a **single service account** (Keycloak client-credentials grant, holding both `READ` and `WRITE` roles); per-user identity is deferred.

## Why

- A BFF lets Express attach the bearer token server-side and re-emit ICM's SSE stream, sidestepping the browser `EventSource` limitation that it cannot send an `Authorization` header.
- Same-origin `/api/*` means no CORS and minimal change to the React components.
- A service account unblocks end-to-end integration without building a login UI, session handling, and a browser OIDC flow now.

## Consequences

- **Every document is owned by the one service principal** — ICM's `creationUser`/owner field carries no per-person meaning, and there is no per-user `READ`/`WRITE` separation. Acceptable for the current single-user product.
- The frontend roadmap calls for multi-user sessions; introducing real per-user OIDC later is **known rework** (login flow, session-bound token relay, per-user ownership backfill).
- The Keycloak realm must define a confidential client for the BFF with `READ` + `WRITE` roles.
