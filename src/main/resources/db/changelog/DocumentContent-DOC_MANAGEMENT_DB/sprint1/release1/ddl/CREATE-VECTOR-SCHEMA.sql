CREATE SCHEMA IF NOT EXISTS vectorcontent;

SET SEARCH_PATH TO vectorcontent;

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS embeddings (
  id SERIAL PRIMARY KEY,
  embedding vector,
  text text,
  created_at timestamptz DEFAULT now()
);