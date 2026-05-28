# Context: Intelligent Document Management — intelligent-content-management

## Bounded context

This service owns **document storage, indexing, and full-text search**. It is one of two services in the Intelligent Document Management system:

| Service | Responsibility |
|---------|---------------|
| `intelligent-content-management` (this service) | Document CRUD, metadata persistence (PostgreSQL), full-text indexing (Elasticsearch), document content extraction (Tika) |
| `dms` (AI microservice) | AI/RAG: document embedding, vector storage (pgvector), question-answering via local Ollama |

The two services communicate over HTTP. `intelligent-content-management` delegates AI operations to `dms` via the `AiServiceClient` REST port.

---

## Glossary

**Document**
A file uploaded by a user. Has metadata (name, owner, location, status, type, size) stored in PostgreSQL and extracted text content indexed in Elasticsearch. A Document may be sent to the AI microservice for embedding into the vector store.

**DocumentAggregate**
The root domain object representing a Document. Encapsulates identity, metadata, and the document's lifecycle state.

**DocumentAttachment**
A file payload associated with a Document, used during upload and content extraction.

**DocumentFileCommand**
A command object carrying the raw file data and metadata needed to create or update a Document.

**ElasticDocument**
The Elasticsearch projection of a Document — the representation indexed for full-text search. Separate from the JPA entity.

**AiServiceClient**
The outbound HTTP port through which this service delegates AI operations (embedding, question-answering) to the `dms` microservice. Contains no AI logic itself.

**DocumentEventProcessor**
Handles domain events asynchronously: `DocumentUploadFileEvent` triggers Elasticsearch indexing; `DocumentSendToVectorStoreEvent` triggers the call to AiServiceClient.

**Answer**
The response value object returned from the AI microservice after a question-answering query.

**Question**
A value object representing a natural-language query sent to the AI microservice.

**ExtractionResult**
The output of a document content extraction operation (via Tika or another extractor). Contains the extracted text.
