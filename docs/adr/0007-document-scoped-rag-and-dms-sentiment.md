# Document-scoped RAG and DMS-computed sentiment

Extending ADR-0001 (all AI lives in DMS), the chat feature becomes **document-scoped retrieval-augmented generation** and **sentiment** becomes a real AI classification owned by DMS. When the user selects documents, ICM passes their `documentId`s through `/ask`, and DMS filters its `similaritySearch` to chunks from those documents (global top-K when none are selected). Chat is **single-shot** (no conversation history). DMS reports the source `documentId`s of the chunks it used, which ICM relays to the frontend as citations.

## Why

- Faithfully reproduces the frontend's "chat about *these* documents" UX, which the current global-only top-K search cannot express.
- Keeps all AI reasoning (retrieval, generation, classification) inside DMS, consistent with the bounded-context boundary.

## Consequences

- DMS must **tag each chunk with its `documentId`** at embedding time and apply a filter expression on retrieval (chunks are untagged today).
- Because content is now editable (ADR-0004), editing a document invalidates its embeddings: DMS needs a **delete-chunks-by-`documentId`** operation, and ICM must re-embed on content change (delete + re-add).
- **Sentiment** is classified by DMS and stored by ICM as Document metadata, computed **asynchronously** via the existing `DocumentEventProcessor` event path; it may be `pending` until DMS responds. This adds a "classify content" capability to DMS, which previously did only embedding and question-answering.
- The SSE contract carries a final metadata event with the referenced `documentId`s alongside the streamed answer text.
- Single-shot Q&A is a deliberate, visible downgrade from the frontend's current multi-turn chat; multi-turn is out of scope for this round.
