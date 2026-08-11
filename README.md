# AI PDF Processor

Upload PDFs, search them by meaning, and land on the exact passage — the viewer
opens the right document, scrolls to the right page, and highlights the matching
text in place.

You do not have to remember the wording. A query like
*"how do they cut inference cost"* surfaces a passage about *"most of the
computation can be done offline"*, which shares almost none of the same words.

## How it works

Every PDF is divided into chunks and every chunk is transformed into embedding vector. 
Your query is also turned into embedding vector the same way, so finding the relevant
passages becomes a nearest-neighbour lookup — which is what Qdrant stores the
vectors to do.

Those embeddings are produced by an AI model running locally in Ollama, alongside
the rest of the stack. The default model is `qwen3-embedding:0.6b` at 512
dimensions, and any other Ollama embedding model can be swapped in through
`EMBEDDING_MODEL`.

Such embeddings alone is a poor way to find a surname or a part number, so search is
hybrid: the dense vector arm runs alongside a BM25 keyword arm for exact terms,
and Qdrant fuses the two rankings.

```
┌──────────────────────────────────────────────────────────────────┐
│  React Frontend  ───────────────────────────►  Spring Boot :8080 │
│                                                     /api/**      │
└──────────────────────────────────────────────────────────────────┘
                                     │
        ┌────────────────────────────┼────────────────────────────┐
        ▼                            ▼                            ▼
   PostgreSQL                    Qdrant                       Ollama
   documents,                dense + BM25                 embeddings
   chunks, highlight         hybrid search
   rectangles
```

## Quick start

Requires Docker with Compose. Nothing else — the image builds the frontend and
the backend itself.

```bash
cp app.env.example app.env      # edit DB_PASSWORD at least
docker compose --env-file app.env up -d --build
```

Then open <http://localhost:8080>.

The first run pulls the embedding model (~600 MB) into Ollama, so give it a few
minutes before uploading. A freshly uploaded PDF is parsed and embedded in the
background: it appears in the list right away as `PROCESSING` and becomes
searchable when it flips to `READY`.

## Local development

Run the dependencies in containers and the app on the host:

```bash
docker compose --env-file app.env up -d postgres qdrant ollama ollama-model-setup
./gradlew bootRun
```

In another shell, start the frontend dev server for hot reload:

```bash
cd frontend
npm install
npm run dev
```

The dev server is on <http://localhost:5173> and proxies `/api` to port 8080, so
the browser still sees a single origin. Point it elsewhere with `BACKEND_URL`.

## Building

`./gradlew bootJar` builds the React app and packs it into the jar under
`static/`, so one artifact serves both the UI and the API:

```bash
./gradlew bootJar
java -jar build/libs/*.jar
```

This needs Node.js 20+ on `PATH`. Pass `-PskipFrontend` to skip the frontend
build and produce an API-only jar — that flag is what the Docker build uses,
since its Node stage has already produced `dist/`.

## Configuration

Every setting is an environment variable read from `app.env`, which is
gitignored. [`app.env.example`](app.env.example) is the annotated template and
the authoritative list; the essentials are:

| Variable | Default | Notes |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | — | Required. No defaults. |
| `DB_NAME` | — | Required by Compose; must match `DB_URL`. |
| `EMBEDDING_MODEL` | `qwen3-embedding:0.6b` | Pulled into Ollama on first start. |
| `EMBEDDING_DIMENSIONS` | `512` | Also the Qdrant vector size — see below. |
| `EMBEDDING_BASE_URL` | `http://localhost:11434` | |
| `QDRANT_HOST` / `QDRANT_PORT` | `localhost` / `6334` |  |
| `STORAGE_LOCAL_ROOT` | `./data/uploads` | Where PDFs are written. |
| `MAX_UPLOAD_SIZE` | `50MB` | Spring's own default of 1MB rejects most PDFs. |
| `SERVER_PORT` | `8080` | |
| `DEV_USER_ID` | `00000000-…-0001` | See *Authentication* below. |

`EMBEDDING_DIMENSIONS` sizes the Qdrant collection as well as the embeddings.
Changing it — or switching to a model with a different output size — leaves the
existing collection unusable; drop the Qdrant volume and re-upload.

Under Compose, the app service overrides `DB_URL`, `QDRANT_HOST`,
`EMBEDDING_BASE_URL`, and `STORAGE_LOCAL_ROOT` with in-network values, so those
entries in `app.env` are the ones a host-run `bootRun` uses.

## Authentication

There is none. `DevelopmentCurrentUserProvider` attributes every request to
`DEV_USER_ID`, and each query is filtered by that id, so the ownership model is
already threaded through the persistence and vector layers — but anyone who can
reach the port is that user. This must be changed in case of deployment.

## API

Every REST endpoint is under `/api` — `WebConfig` applies the prefix to the
controllers, so the root stays free for the SPA and its assets. The one mapped
path outside `/api` is Spring's own `/error`.

An unknown path returns a JSON 404 rather than `index.html`. That is correct
while the SPA has no client-side router; add a forwarding rule before you
introduce one, or deep links will 404.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/documents` | Upload a PDF (multipart `file`). Returns immediately; parsing is async. |
| `GET` | `/api/documents` | Paged list with processing status. |
| `GET` | `/api/documents/{id}` | One document, for polling status. |
| `GET` | `/api/documents/{id}/file` | The PDF itself, inline. |
| `GET` | `/api/documents/{id}/chunks` | Every chunk of a document. |
| `GET` | `/api/documents/chunks/{chunkId}` | A chunk with its page highlight rectangles. |
| `DELETE` | `/api/documents/{id}` | Remove the file, its rows, and its vectors. |
| `GET` | `/api/search` | `query`, optional `documentId`, `limit`, `minimumScore`. |

A search hit carries only its chunk id and text. The rectangles needed to draw
the highlight come from `/api/documents/chunks/{chunkId}`, which is why opening
a result takes two calls.

## Layout

```
src/main/kotlin/org/ai_processor/
  document/          controllers, services, JPA entities
  processing/
    pdfreader/       OpenDataLoader parsing into typed blocks with bounding boxes
    chunking/        blocks grouped into chunks, carrying their rectangles
    embeddings/      Ollama client
  vector_storage/    Qdrant client
  storage/           PDF storage on disk
frontend/            React + Vite SPA (see frontend/README.md)
```

## Tests

```bash
./gradlew test
```

Unit-level and hermetic — no Postgres, Qdrant, or Ollama required.

## License

MIT. See [LICENSE](LICENSE).
