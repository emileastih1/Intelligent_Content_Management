# Context: Intelligent Document Management — intelligent-content-management

## Bounded context

This service owns **document storage, indexing, and full-text search**. It is one of two services in the Intelligent Document Management system:

| Service | Responsibility |
|---------|---------------|
| `intelligent-content-management` (this service) | Document CRUD, metadata persistence (PostgreSQL), full-text indexing (Elasticsearch), document content extraction (Tika) |
| `dms` (AI microservice) | AI/RAG: document embedding, vector storage (pgvector), question-answering, and content classification (sentiment) via local Ollama |

The two services communicate over HTTP. `intelligent-content-management` delegates AI operations to `dms` via the `AiServiceClient` REST port.

---

## Glossary

**Document**
A unit of textual content owned by a user. Its editable **content** is the first-class field, alongside metadata (name, owner, location, status, type, size). Content can be authored directly or seeded by uploading a file, in which case the file's text is extracted into the content. Persisted in PostgreSQL and indexed in Elasticsearch for full-text search. A Document may be sent to the AI microservice for embedding into the vector store.
_Avoid_: File — a file is one way to seed a Document's content, not the Document itself.

**Ingestion path**
How a Document's content gets populated: either **authored** (typed directly) or **uploaded** (a file is provided and Tika extracts its text into the content). Both paths converge on the same content-first Document. Once content is set, it is this `TEXT_CONTENT` — not the original file — that is sent to DMS for vector-store embedding.

**DocumentAggregate**
The root domain object representing a Document. Encapsulates identity, metadata, and the document's lifecycle state.

**DocumentAttachment**
A file payload associated with a Document, used during upload and content extraction.

**DocumentFileCommand**
A command object carrying the raw file data and metadata needed to create or update a Document.

**ElasticDocument**
The Elasticsearch projection of a Document — the representation indexed for full-text search. Separate from the JPA entity.

**AiServiceClient**
The outbound HTTP port through which this service delegates AI operations (embedding, question-answering, sentiment classification) to the `dms` microservice. Contains no AI logic itself.

**Tag**
A free-form, user-applied label on a Document. A Document may have many. Used for filtering and grouping.

**Category**
A single classification assigned to a Document (e.g. Report, Architecture). Unlike Tags, a Document has exactly one.

**Sentiment**
An AI-derived classification of a Document's content (e.g. Positive, Neutral, Critical, Analytical, Informative), produced by DMS and stored by ICM. Recomputed asynchronously when content changes; may be `pending` until DMS responds.
_Avoid_: tone, mood.

**DocumentEventProcessor**
Handles domain events asynchronously: `DocumentUploadFileEvent` triggers Elasticsearch indexing; a content-embed event triggers the call to AiServiceClient with the document's `TEXT_CONTENT` (not the original file); `DocumentDeleteFromVectorStoreEvent` purges chunks from DMS; `DocumentClassifySentimentEvent` triggers AI sentiment classification.

**Answer**
The response value object returned from the AI microservice after a question-answering query.

**Question**
A value object representing a natural-language query sent to the AI microservice.

**ExtractionResult**
The output of a document content extraction operation (via Tika or another extractor). Contains the extracted text.
