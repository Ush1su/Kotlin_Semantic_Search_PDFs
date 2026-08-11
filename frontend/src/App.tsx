import { useCallback, useEffect, useRef, useState } from 'react'
import { getChunkHighlight } from './api/client'
import type { DocumentResponse, SearchResult } from './api/types'
import { Sidebar } from './components/Sidebar'
import { PdfViewer } from './components/PdfViewer'
import type { ActiveHighlight, HighlightSource } from './components/PdfViewer'
import { useDocuments } from './hooks/useDocuments'
import { useSearch } from './hooks/useSearch'
import { errorMessage } from './lib/errors'

const GLOBAL_SEARCH = { limit: 5, minimumScore: 0.25 }
const IN_DOCUMENT_SEARCH_LIMIT = 10

export default function App() {
  const documents = useDocuments()
  const [openDocumentId, setOpenDocumentId] = useState<string | null>(null)
  const [highlight, setHighlight] = useState<ActiveHighlight | null>(null)
  const [chunkError, setChunkError] = useState<string | null>(null)
  const nonceRef = useRef(0)

  const globalSearch = useSearch(GLOBAL_SEARCH)
  const inDocumentSearch = useSearch({
    limit: IN_DOCUMENT_SEARCH_LIMIT,
    minimumScore: GLOBAL_SEARCH.minimumScore,
    documentId: openDocumentId ?? undefined,
  })

  /**
   * A search hit only carries its chunk id, so the rectangles to highlight come
   * from a second call. The viewer then opens that document and scrolls to them.
   */
  const showResult = useCallback(
    async (result: SearchResult, source: HighlightSource) => {
      setChunkError(null)

      try {
        const chunk = await getChunkHighlight(result.chunk.chunkId)

        documents.ensureDocument(chunk.documentId)
        setOpenDocumentId(chunk.documentId)
        setHighlight({
          chunkId: chunk.chunkId,
          documentId: chunk.documentId,
          rects: chunk.highlight,
          source,
          nonce: ++nonceRef.current,
        })
      } catch (caught) {
        setChunkError(errorMessage(caught))
      }
    },
    [documents],
  )

  const showGlobalResult = useCallback(
    (result: SearchResult) => void showResult(result, 'global'),
    [showResult],
  )

  const showDocumentResult = useCallback(
    (result: SearchResult) => void showResult(result, 'document'),
    [showResult],
  )

  // A highlight belongs to the results list it came from: close that list —
  // whether by the clear button or by emptying the box — and the marks go too.
  const sourceStatus =
    highlight?.source === 'document' ? inDocumentSearch.status : globalSearch.status

  useEffect(() => {
    if (highlight && sourceStatus === 'idle') {
      setHighlight(null)
    }
  }, [highlight, sourceStatus])

  const openDocument = useCallback((documentId: string) => {
    setOpenDocumentId(documentId)
    setHighlight(null)
    setChunkError(null)
  }, [])

  const deleteDocument = useCallback(
    async (document: DocumentResponse) => {
      if (!window.confirm(`Delete ${document.originalFilename}?`)) {
        return
      }

      if (document.id === openDocumentId) {
        setOpenDocumentId(null)
        setHighlight(null)
      }

      await documents.remove(document.id)
    },
    [documents, openDocumentId],
  )

  const open = openDocumentId ? documents.byId.get(openDocumentId) : undefined

  return (
    <div className="app">
      <header className="app-header">
        <h1>AI PDF</h1>
        <form className="global-search" onSubmit={(event) => event.preventDefault()}>
          <input
            type="search"
            value={globalSearch.query}
            placeholder="Search across all documents…"
            onChange={(event) => globalSearch.setQuery(event.target.value)}
          />
        </form>
      </header>

      <main className="app-body">
        <Sidebar
          documents={documents}
          search={globalSearch}
          openDocumentId={openDocumentId}
          activeChunkId={highlight?.chunkId ?? null}
          onOpenDocument={openDocument}
          onSelectResult={showGlobalResult}
          onDeleteDocument={deleteDocument}
        />

        {openDocumentId ? (
          <PdfViewer
            key={openDocumentId}
            documentId={openDocumentId}
            filename={open?.originalFilename ?? 'Document'}
            highlight={highlight}
            search={inDocumentSearch}
            onSelectResult={showDocumentResult}
          />
        ) : (
          <section className="viewer viewer-empty">
            <p>Select a document, or search to jump straight to a passage.</p>
          </section>
        )}
      </main>

      {chunkError ? <div className="toast error">{chunkError}</div> : null}
    </div>
  )
}
