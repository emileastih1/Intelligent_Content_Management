# ADR 0003: SSE streaming for the document ask endpoint

## Status

Accepted (implementation deferred — after RAG eval script)

## Context

The current `POST /idm/api/v1/document/ask` endpoint in ICM proxies to DMS
(`POST /AiServiceClient/v1/document/ask`), which calls Ollama (gemma3:4b) and returns
a complete JSON response once the model has finished generating:

```
Frontend → ICM → DMS → Ollama (blocking) → DMS → ICM → Frontend
```

This creates a poor UX: the user submits a question and waits in silence until the full
answer is ready — typically several seconds. The target UX is progressive streaming
(tokens appear as generated), identical to how Claude, ChatGPT, and similar interfaces work.

### Alternatives considered

| Option | Protocol | Direction | Notes |
|--------|----------|-----------|-------|
| A — SSE | HTTP `text/event-stream` | Server → client (unidirectional) | Fits ask pattern perfectly; HTTP-native |
| B — WebSocket | WS | Bidirectional | Necessary only if client sends data mid-stream |
| C — Polling | HTTP | Client pulls | High latency, chatty, poor UX |
| D — Client calls DMS directly | — | — | Bypasses ICM auth layer (Keycloak) |

## Decision

Use **Server-Sent Events (SSE)** for streaming, propagated through the full chain:

```
Frontend → ICM (text/event-stream) → DMS (text/event-stream) → Ollama (streaming)
```

- **DMS** changes `POST /AiServiceClient/v1/document/ask` to return `Flux<String>`
  with `produces = text/event-stream`, using Spring AI's `ChatClient.stream()`.
- **ICM** changes its proxy to consume DMS's `Flux<String>` via `WebClient` and
  re-emit it as SSE to the caller, preserving Keycloak auth at the ICM boundary.
- **Frontend** (future) connects using `EventSource` or `fetch` with `ReadableStream`.

WebSocket was rejected: the ask flow is unidirectional (one question → token stream).
Bidirectional capability adds protocol complexity with no benefit.

Direct client→DMS was rejected: DMS is an internal service. All client traffic must pass
through ICM's Keycloak-enforced security layer.

## Consequences

- The API contract for `POST /idm/api/v1/document/ask` changes from
  `application/json` → `text/event-stream`. Existing synchronous callers
  (integration tests, eval script) must be updated to consume the stream or use
  a compatibility wrapper.
- ICM requires `WebClient` (already available via Spring WebFlux) to proxy the stream;
  the current `RestTemplate`/`RestClient` call must be replaced for this endpoint.
- WireMock stubs in `DocumentRagIntegrationTest` must be updated to return chunked
  SSE responses rather than a single JSON body.
- DMS's `DocumentAiRepository.askQuestion()` switches from blocking
  `call().content()` to `stream().content()`, returning `Flux<String>`.
- Eval script must accumulate the SSE stream before applying keyword-match assertions.
