# Content-first Document model

ICM originally modelled a **Document** as *a file uploaded by a user*, whose text was a read-only Tika **extraction** indexed for search. To let the frontend author and edit document text in place (and to make ICM grow to match the frontend's product), we redefine a Document so that its editable **content** is the first-class field. Uploading a file becomes *one ingestion path* — Tika extracts the file's text *into* content — while authoring text directly is the other. Both converge on the same aggregate.

## Considered Options

- **Content-first Document (chosen)** — one aggregate; uploaded files and authored notes are unified; editing updates content directly.
- **Two concepts (Document + Note)** — keep file-Document read-only, add a separate editable Note. Cleaner semantics but doubles the backend and forces the UI to merge two lifecycles.
- **Authored text as a synthetic file** — wrap typed text as a fake `.md` file through the upload→Tika pipeline. Least conceptual change but every edit re-synthesises and re-extracts the file, and content is always a derived copy.

## Consequences

- Inverts the previous "content is extracted, read-only" rule; indexing now treats content as the source of truth.
- The original uploaded binary is no longer the system of record — it only seeds content. (If a compliance "the signed file is the record" requirement ever appears, this must be revisited — it would push back toward the two-concept option.)
- Editing content invalidates downstream derivations (search index, vector-store embeddings) and must trigger re-indexing/re-embedding — see ADR-0007.
