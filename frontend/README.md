# AI PDF — frontend

A single-page React + Vite client for the Spring Boot backend: upload PDFs, read
them, and jump straight to the passage a semantic search found.

## Running

The backend must be up first (`docker compose --env-file app.env --profile full up`
from the repository root), then:

```bash
npm install
npm run dev
```

The dev server listens on <http://localhost:5173> and proxies `/api` to
`http://localhost:8080`, which is where the backend mounts everything
(`server.servlet.context-path`). Point the proxy elsewhere with `BACKEND_URL`.

## How it fits together

- `api/` — typed mirrors of the Kotlin DTOs plus thin `fetch` wrappers.
- `hooks/useDocuments` — the document list. Processing is asynchronous on the
  backend, so the list re-polls while any document is `UPLOADED` or `PROCESSING`.
- `hooks/useSearch` — one hook for both searches; passing a `documentId` scopes it
  to the open PDF. Global search asks for the 5 best hits above a 0.25 score.
- `components/PdfViewer` — reads every page's dimensions up front so unrendered
  pages still hold their space, then mounts only the pages near the viewport.
  That keeps a jump to page 200 both instant and accurate.
- `lib/highlight.ts` — converts a chunk's stored bounding box into a CSS rect.

Clicking a search hit takes two calls: the hit carries only a chunk id, so
`GET /documents/chunks/{chunkId}` supplies the rectangles and the page to scroll to.
