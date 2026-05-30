# PRD: Close the three gaps blocking an end-to-end frontend demo

> Depends on PRD #68 (frontend → ICM wiring, issues #69–#78) being complete.
> Gap 3 (manual service startup) is already covered by PRD #79 and is out of scope here.
> Respects ADR-0001 (AI in DMS), ADR-0004 (content-first Document), ADR-0005 (BFF service account), ADR-0007 (doc-scoped RAG).

## Problem Statement

A user who opens the frontend application today cannot get a working demo, despite the backend being fully wired from PRD #68. Three things block them:

1. **The BFF cannot authenticate to ICM.** The BFF uses a service-account token (ADR-0005), but the required Keycloak client (`icm-bff`) does not exist in the realm. Every call from the BFF to ICM returns 401, so the document list never loads and nothing works.

2. **Chat never answers questions about typed documents.** When a user authors a document by typing text, the content is saved in Postgres but never sent to DMS for embedding. The vector store stays empty for that document, so RAG returns "Sorry, I don't have an answer." Only documents uploaded as binary files ever get embedded — and even then, DMS runs Tika a second time on the raw file bytes despite ICM already having the extracted text. This contradicts the content-first model (ADR-0004) which states `TEXT_CONTENT` is the source of truth.

3. **Editing a document leaves stale embeddings.** When a user edits a document's content, the vector store is not updated. Future chat answers draw on outdated chunks from before the edit.

## Solution

Fix all three gaps so that after a `docker compose up` (PRD #79) or a manual startup, a user can:
- Open the frontend and see their documents (BFF authenticates successfully)
- Type a document, ask a question about it in the chat tab, and receive a grounded answer
- Edit a document and immediately have chat reflect the new content

The solution has two parts. First, add the `icm-bff` Keycloak client to the realm so the BFF can obtain a service-account token. Second, fix the embedding path so that **all** documents — authored and uploaded — are embedded using their `TEXT_CONTENT` (not the raw file), and re-embedded whenever that content changes.

## User Stories

1. As a user, I want to open the frontend and immediately see my document list, so that I know the application is connected to the real backend.
2. As a user, I want to type a new document, save it, and ask a question about it in the chat tab, so that I can use the AI over my own authored content.
3. As a user, I want an uploaded document to also be searchable via chat, so that both ways of creating a document work the same way.
4. As a user, I want editing a document's content and saving to immediately update what the chat assistant knows, so that I don't get stale answers after I make changes.
5. As a user, I want the sentiment badge on a document to fill in shortly after I save, so that AI classification feels live and responsive.
6. As a developer, I want the BFF service account to have READ and WRITE roles in Keycloak, so that it can call all ICM endpoints without per-user login.
7. As a developer, I want the BFF Keycloak client to be part of the committed realm export, so that it is created automatically on every fresh stack start.
8. As a developer, I want DMS to accept plain text content for embedding (no file required), so that authored documents are treated identically to uploaded ones in the vector store.
9. As a developer, I want ICM to always send `TEXT_CONTENT` to DMS for embedding — not the raw file bytes — so that the embedding path is consistent with the content-first model (ADR-0004).
10. As a developer, I want the old file-based embedding endpoint in DMS to be deprecated, so that there is a single, correct embedding path.
11. As a developer, I want re-embedding to fire on every content change (edit), so that the vector store always reflects current document text.
12. As a developer, I want re-embedding to be: delete-old-chunks first, then embed-new-content, so that stale vectors are never mixed with fresh ones.
13. As a developer, I want re-embedding to be async (non-blocking), so that saves stay fast even on large documents.
14. As a developer, I want the new DMS embed-content endpoint to tag chunks with `documentId`, so that doc-scoped RAG (ADR-0007) continues to work correctly.

## Implementation Decisions

### Gap 1 — `icm-bff` Keycloak client

- A new confidential Keycloak client `icm-bff` is added to the realm export with `serviceAccountsEnabled: true` and `directAccessGrantsEnabled: false`. It uses client-credentials grant only.
- A secret is set (e.g. `icm-bff-secret`) and recorded in the BFF environment (`.env.example`).
- The `icm-bff` service account is granted `READ` and `WRITE` client roles from the existing `intelligent-content-management-api` client via a scope mapping. This means ICM's `@PreAuthorize("hasRole('READ')")` / `WRITE` checks work unchanged — the token simply carries those role claims.
- No changes to ICM's security configuration are required.

### Gap 2 — Content-first embedding path

**DMS: new `ContentEmbedder` capability**
- New endpoint `POST /v1/document/embed-content` accepts `{documentId, documentName, content}` as JSON. No file, no Tika. DMS splits the plain text directly using the existing `TokenTextSplitter` (400-token chunks), tags each chunk with `documentId` metadata (ADR-0007), and adds to the vector store.
- The old `POST /v1/document` (file-based embedding) is deprecated — left in place for backward compatibility but no longer called by ICM.
- The `DocumentAiClientRepository`, `AiServiceClient` interface, and their implementations gain `embedContent(documentId, documentName, content)`.

**ICM: all documents embed via `TEXT_CONTENT`**
- `DocumentAiRestClient` gains an `embedContent(documentId, name, content)` method that calls the new DMS endpoint.
- `AiServiceClientCommandPort` and its adapter gain `embedContent`.
- On document **create** (both authored and uploaded ingestion paths): after persisting and after `TEXT_CONTENT` is set, fire an async `DocumentEmbedContentEvent` carrying `documentId`, `documentName`, and `content`. The `DocumentEventProcessor` handles this event by calling `embedContent` via `AiServiceClientCommandPort`. This replaces the old `DocumentSendToVectorStoreEvent` for ICM → DMS embedding.
- `DocumentSendToVectorStoreEvent` is deprecated (retained for backward compatibility, but no longer fired for new documents).

### Gap 3 — Re-embed on content edit

- On document **update** (when `content` changes): the `DocumentJpaAdapter` fires `DocumentDeleteFromVectorStoreEvent` (delete old chunks) followed by `DocumentEmbedContentEvent` (embed new content). Both are async via `DocumentEventProcessor`.
- This is the same pattern already used for the sentiment re-classification event. The three async reactions to a content change are: Elasticsearch re-index, vector-store re-embed (delete + embed), and sentiment re-classification — all independent, all non-blocking.
- Order within the delete-then-embed pair is enforced by the event handlers (delete fires first, then embed is published after confirmation).

## Testing Decisions

A good test asserts observable behavior through the module's public interface — not call sequences or internal state. It survives a refactor that preserves behavior and uses realistic inputs.

**DMS: `ContentEmbedder` (new deep module)**
- Embedding plain text: calling `embed-content` with a known `documentId` and content results in chunks tagged with that `documentId` appearing in the vector store. Subsequent `similaritySearch` with a filter on that `documentId` returns results.
- Re-embed: calling delete-by-documentId followed by embed-content yields only the new chunks (no stale vectors).
- Prior art: `DocumentAiRepositoryTest`, `DocumentAiRepositoryIT`.

**ICM: `DocumentJpaAdapter` + `DocumentEventProcessor`**
- Authored document: creating a document with content (no file) fires an embed event; the async handler calls DMS embed-content with the correct `documentId` and `content`.
- Uploaded document: creating a document with a file also fires an embed event using the extracted `TEXT_CONTENT` (not the raw file bytes).
- Content edit: updating a document's content fires a delete event and then an embed event; the vector store reflects the new content after both handlers run.
- Prior art: `DocumentRagIntegrationTest` (cycles 7–12 already test create/edit/upload paths), `AbstractRestTest`, `KeycloakSecurityIntegrationTest`.

## Out of Scope

- **Gap 3 (manual service startup):** Already covered by PRD #79 (issues #80–#85). This PRD's fixes apply whether services are started manually or via Docker compose.
- **Per-user authentication:** The BFF remains a single service account (ADR-0005). Multi-user identity is deferred.
- **Keycloak login UI:** No browser-facing login flow is added; the service account token is invisible to the user.
- **Removing the deprecated file-based DMS endpoint:** The old `POST /v1/document` stays for backward compatibility. Removal is a separate cleanup task.
- **Increasing the Awaitility timeout on the flaky `uploadDocIndexesInElasticsearch` test:** The async ES indexing timing issue is pre-existing and independent of these gaps.

## Further Notes

- The `icm-bff` client secret must be consistent between the realm export and the BFF environment (`.env` / `.env.example`). The secret is not sensitive in a local-dev context; for production it must be rotated and injected via secrets management.
- The `DocumentEmbedContentEvent` deprecates `DocumentSendToVectorStoreEvent` as the ICM → DMS embedding trigger. Both names should coexist briefly during the transition; `DocumentSendToVectorStoreEvent` can be fully removed in a follow-up cleanup once all callers use the new event.
- The three async reactions on content change (Elasticsearch re-index, vector-store re-embed, sentiment re-classify) are all fired by `DocumentJpaAdapter` and handled by `DocumentEventProcessor`. They are independent — a DMS outage does not block Elasticsearch indexing.
- Gap 3 (startup) blocks a demo in the absence of PRD #79. Developers can still run a manual demo today by starting the infra compose, Ollama, DMS, ICM, and the frontend individually, provided gaps 1 and 2 are closed first.
