# ADR 0001: AI capability extracted to the DMS microservice

## Status

Accepted

## Context

Early versions of this service included Spring AI dependencies and OpenAI configuration (`OPENAI_API_KEY`, `gpt-3.5-turbo-1106`) with the intent of calling OpenAI directly from this codebase. As the AI/RAG requirements grew (vector storage, embedding models, local Ollama support, PgVector), embedding that complexity here would have mixed two distinct concerns: document management and AI inference.

## Decision

All AI capability — Spring AI, embedding models, vector store, RAG pipeline — was extracted into a dedicated microservice (`dms` / `SpringAiServiceClient`). This service retains only the outbound HTTP port (`AiServiceClient`) that delegates AI operations over REST.

The `spring-ai.version` property and all OpenAI configuration are removed from this codebase. They are not used and create a misleading impression that AI runs here.

## Consequences

- This service has no Spring AI dependency and no AI inference logic. Adding AI features here in future requires a deliberate decision.
- The `dms` microservice is a hard runtime dependency for any AI-related endpoint (document embedding, question-answering). If `dms` is unavailable, those endpoints degrade.
- The boundary is clean: document lifecycle is owned here; AI lifecycle is owned by `dms`.
