# PRD: Frontend → ICM wiring (content-first Documents, BFF, document-scoped RAG)

> Blocking prerequisite for the global-compose / containerization work. Respects ADR-0004 (content-first Document), ADR-0005 (Express BFF + service-account auth), ADR-0007 (document-scoped RAG + DMS sentiment), and ADR-0001 (all AI lives in DMS).

## Problem Statement

We have a polished frontend (React + an Express server) that lets a user create, edit, tag, categorise, list, and chat about documents — but it is a self-contained island. It stores documents in a local JSON file and calls Gemini directly. None of that touches our real system: ICM (document storage, indexing, full-text search) and DMS (embedding, vector store, RAG). So the product the user sees is disconnected from the platform we have actually built, and nothing the user does in the UI is persisted, searched, secured, or reasoned over by our services.

We want the frontend to become a true client of our platform: every document the user authors or uploads lives in ICM, is indexed and embedded, and chat answers come from our own document-scoped RAG over DMS — not from a third-party model over a throwaway JSON file.

## Solution

Wire the frontend to ICM through the existing Express server acting as a **Backend-For-Frontend (BFF)**. The browser keeps calling its same-origin `/api/*` surface; the BFF authenticates to ICM with a service account and proxies every call to ICM, including relaying ICM's streaming chat.

To make this possible, ICM **grows to match the frontend's product**: the **Document** becomes content-first (editable `content` as a first-class field, with file upload as one *Ingestion path*), and gains **Tag**, **Category**, and **Sentiment**. ICM exposes the CRUD, list, and search operations the frontend needs. Chat becomes **document-scoped RAG**: the user's selected documents constrain retrieval, answers stream back over SSE, and the **Sentiment** of a document is an AI verdict produced by DMS and stored by ICM.

The end state: the user uses the same UI, but documents are real (persisted, indexed, embedded, secured), search is real (Elasticsearch), and chat is grounded in the user's own documents via DMS — with citations.

## User Stories

1. As a user, I want to author a new text document in the editor and have it saved in ICM, so that my documents persist beyond a local file.
2. As a user, I want to upload a file (e.g. PDF) and have its text extracted into the document's content, so that uploaded and authored documents behave identically.
3. As a user, I want to edit a document's content and save it, so that I can refine documents over time.
4. As a user, I want my edits to a document to update its search index entry, so that search always reflects the current content.
5. As a user, I want my edits to a document to re-embed it in the vector store, so that chat answers reflect the current content and never cite stale text.
6. As a user, I want to delete a document, so that it disappears from listings, search, and chat retrieval.
7. As a user, I want to see a list of all my documents, so that I can browse what I have.
8. As a user, I want to open a document and see its full content, so that I can read and edit it.
9. As a user, I want to add free-form tags to a document, so that I can organise documents my own way.
10. As a user, I want to assign a single category to a document, so that I can classify it at a glance.
11. As a user, I want to apply tags and/or a category to several selected documents at once, so that I can organise in bulk.
12. As a user, I want each document to show a sentiment label derived by AI, so that I can gauge the tone of a document without reading it.
13. As a user, I want a newly saved document to show its sentiment as `pending` and then fill in shortly after, so that saving stays fast while classification happens in the background.
14. As a user, I want to run a full-text search across my documents, so that I can find documents by their words, not just titles.
15. As a user, I want to select one or more documents and chat about specifically those, so that answers are grounded only in the documents I care about.
16. As a user, I want to chat with no documents selected and get answers grounded across my whole corpus, so that I can ask broad questions.
17. As a user, I want chat answers to stream in token by token, so that the experience feels responsive.
18. As a user, I want chat answers to show which documents they drew from (citations), so that I can trust and verify the answer.
19. As a user, I want chat to work without me logging in (for now), so that I can use the product immediately.
20. As a developer, I want the frontend to stop calling Gemini and the local JSON store, so that there is a single source of truth (ICM/DMS).
21. As a developer, I want the BFF to authenticate to ICM with a service account, so that ICM's secured endpoints can be called without a per-user login flow yet.
22. As a developer, I want the BFF to acquire and refresh its service-account token automatically, so that the integration does not break when tokens expire.
23. As a developer, I want the BFF to relay ICM's SSE chat stream to the browser, so that streaming works despite `EventSource` not supporting an `Authorization` header.
24. As a developer, I want ICM to validate the service-account token's roles (`READ`/`WRITE`), so that the security model is exercised end to end.
25. As a developer, I want DMS chunks tagged with their source `documentId`, so that retrieval can be filtered to selected documents and answers can report citations.
26. As a developer, I want DMS to delete a document's chunks by `documentId`, so that re-embedding on edit does not leave stale vectors.
27. As a developer, I want DMS to expose a sentiment-classification capability, so that ICM can delegate sentiment rather than computing AI itself.
28. As a developer, I want ICM's `/ask` to accept `documentIds` and forward them to DMS, so that document scoping is honoured end to end.
29. As a developer, I want the chat contract to be single-shot (no conversation history), so that the first integration stays simple.
30. As a developer, I want the BFF's `/api/*` response shapes to match what the React components already expect, so that the UI changes are minimal.

## Implementation Decisions

### Model (ADR-0004)
- **Document is content-first.** `DocumentAggregate` gains an editable `content` field as first-class state, plus `tags` (many), `category` (one), and `sentiment`. A Liquibase migration adds the columns; the JPA entity and the `ElasticDocument` projection are updated to carry `content` and the classification fields.
- **Two ingestion paths converge on content.** Authoring sets `content` directly. Uploading provides a file whose text is extracted (Tika) **into** `content`. After ingestion the two are indistinguishable.

### ICM API surface (base path `/idm`, all secured, service-account token carries `READ`/`WRITE`)
- `POST /v1/document` — create a Document. Body supports both ingestion paths: authored content, or an uploaded file (base64 + type) that is extracted into content. Returns the created Document including its id.
- `GET /v1/document` — list Documents (paginated). Returns metadata + classification (not necessarily full content).
- `GET /v1/document/{id}` — get a single Document **including content**.
- `PUT /v1/document/{id}` — update content and/or metadata (title/name, tags, category).
- `POST /v1/document/batch-update` — apply tags-to-add and/or a category to a set of `documentIds`.
- `DELETE /v1/document/{id}` — delete a Document.
- `GET /v1/document/search?q=...` — full-text search over content via Elasticsearch.
- `POST /v1/document/ask?topK=&temperature=` — **grows** to accept `documentIds` (scope) in addition to the `Question`; streams the answer as SSE and emits the referenced `documentId`s as a final metadata event.

### ICM event-driven reactions (extend `DocumentEventProcessor`)
- On create/content-change: raise the existing `DocumentUploadFileEvent` (Elastic re-index) and `DocumentSendToVectorStoreEvent` (DMS embed). Embedding on edit must **delete then re-add** the document's chunks.
- On create/content-change: additionally trigger **async sentiment classification** via `AiServiceClient`; persist the returned value; document is saved immediately with `sentiment = pending`.
- On delete: purge the document from Elasticsearch and from the DMS vector store (delete chunks by `documentId`).

### ICM → DMS port (`AiServiceClientQuery` / adapter) grows
- `streamAnswer(question, documentIds, topK, temperature)` — forwards scope and relays citations.
- `classifySentiment(content) → Sentiment` — new.
- `deleteFromVectorStore(documentId)` — new.

### DMS changes (ADR-0007, ADR-0001)
- **`DocumentChunkIndexer`** (deep, new): on add, attach `documentId` as chunk metadata; expose `deleteByDocumentId(documentId)`. Wraps Tika reader + token splitter + `VectorStore`.
- **`ScopedRagQueryService`** (grow `DocumentAiRepository`): `similaritySearch` builds a filter expression on `documentId` metadata when `documentIds` are provided (global otherwise); collects the distinct source `documentId`s of retrieved chunks and returns them with the stream.
- **`SentimentClassifier`** (deep, new): `classify(content) → Sentiment` via a single Ollama chat call with a constrained prompt returning one of the known labels.
- DMS REST surface: `/v1/document/ask` accepts `documentIds`; new sentiment-classify endpoint; `DELETE /v1/document/{documentId}` to remove chunks.
- SSE contract: text chunks stream as today; a final event carries the referenced `documentId`s before `[DONE]`.

### BFF (Express `server.ts`) — ADR-0005
- **`ServiceAccountTokenProvider`** (deep, new): client-credentials grant against Keycloak; caches the token and refreshes before expiry.
- **`IcmClient`** (deep, new): typed methods mirroring the ICM endpoints above; attaches the bearer token; for `ask`, opens the SSE request to ICM and exposes the stream.
- **BFF routes**: rewrite `/api/documents` (list/create), `/api/documents/:id` (get/update/delete), `/api/documents/batch-update`, add `/api/documents/search`, and `/api/chat/stream` to delegate to `IcmClient`. Remove the local JSON store and all `@google/genai` usage. `/api/chat/stream` consumes ICM's SSE and re-emits to the browser, including the citations event. Response shapes preserved to match existing React components.
- Frontend React: chat sends `documentIds` and stops sending history; renders streamed text + citations; the sentiment badge renders the API value (including a `pending` state) instead of the local keyword heuristic.

### Auth model (ADR-0005)
- Single service account (Keycloak confidential client, client-credentials) with `READ` + `WRITE` roles. All Documents are owned by this one principal for now; per-user identity is deferred (known rework). Keycloak issuer handling per ADR-0006 (split issuer-uri / jwk-set-uri) applies when containerized.

## Testing Decisions

A good test asserts **external, observable behavior** through a module's public interface, not its internals. It should survive a refactor that preserves behavior, use realistic inputs, and avoid asserting on private collaborators or call sequences. The two test scopes below were selected; the BFF client and React components are explicitly **not** in scope for specified tests this round.

### DMS AI modules
- **`DocumentChunkIndexer`**: indexing a document tags every resulting chunk with the correct `documentId`; `deleteByDocumentId` removes exactly that document's chunks and leaves others intact; re-indexing the same `documentId` (delete + add) yields only the new chunks. Prior art: `DocumentAiRepositoryTest` / `DocumentAiRepositoryIT`, `TikaDocumentReaderTest`.
- **`ScopedRagQueryService`**: with `documentIds` provided, retrieval is constrained to those documents; with none, retrieval is global; the returned referenced `documentId`s correspond to the chunks actually used; empty-retrieval returns the "no answer" path. Prior art: `AiServiceClientImplTest`, `DocumentDocumentAiAdapterTest`, `AiServiceClientRestControllerTest`.
- **`SentimentClassifier`**: returns a value from the known label set for representative content; degrades sensibly when the model output is unexpected. Prior art: `AiServiceClientImplTest`.

### ICM services + events
- **`DocumentManagementCommandService`**: create via both ingestion paths persists `content`; `updateContent` updates `content`; `updateClassification` / batch-update applies tags/category to the right documents; `delete` removes the document. Behavioral, against the domain.
- **`DocumentManagementQueryService`**: `list` returns persisted documents (paginated); `getWithContent` returns content; `search` matches on content via Elasticsearch. Prior art: `DocumentQueryRestControllerTest`, `DocumentRagIntegrationTest`.
- **`DocumentEventProcessor`**: a content change triggers Elastic re-index, vector-store delete-then-add, and an async sentiment classification whose result is persisted; a delete purges both Elastic and the vector store. Prior art: `DocumentRagIntegrationTest` (end-to-end RAG path), `AbstractRestTest` (secured REST harness), `KeycloakSecurityIntegrationTest` (role enforcement).

## Out of Scope

- **Per-user authentication and ownership.** The BFF uses one service account; real OIDC login and per-user `creationUser`/roles are deferred (ADR-0005).
- **Multi-turn chat history.** Chat is single-shot this round (ADR-0007).
- **Full-content prompt stuffing.** Selected-document chat uses filtered RAG, not stuffing whole documents into the prompt.
- **Containerization / global compose.** Dockerfiles, the umbrella repo, the DMS move, and env-var wiring are a separate, dependent PRD (ADR-0006).
- **Keeping the original uploaded binary as the system of record.** Files only seed content (ADR-0004).
- **Specified automated tests for the BFF and React UI.** May be added later; manual verification for this round.

## Further Notes

- The async sentiment path reuses ICM's existing `@Async @TransactionalEventListener` mechanism (the same spine that already drives Elastic indexing and vector-store sends), so no new infrastructure is introduced for it.
- Editing content has a three-way fan-out (Elastic re-index, vector-store re-embed, sentiment re-classify); all three are async reactions to the same content-change event and must each be idempotent with respect to repeated edits.
- The SSE citations event is additive to ICM's existing streaming `/ask` (ADR-0003); the browser already consumes an SSE stream today, so the frontend change is to parse the citations event and drop the Gemini-specific framing.
- This PRD is the blocking prerequisite for the global-compose PRD; sequence the work so the BFF and the grown ICM/DMS contracts exist before containerizing.
