import type { DocumentResponse, SearchResult } from '../api/types'
import type { DocumentsState } from '../hooks/useDocuments'
import type { SearchState } from '../hooks/useSearch'
import { DocumentList } from './DocumentList'
import { SearchResults } from './SearchResults'
import { UploadButton } from './UploadButton'

interface SidebarProps {
  documents: DocumentsState
  search: SearchState
  openDocumentId: string | null
  activeChunkId: string | null
  onOpenDocument: (documentId: string) => void
  onSelectResult: (result: SearchResult) => void
  onDeleteDocument: (document: DocumentResponse) => void
}

export function Sidebar({
  documents,
  search,
  openDocumentId,
  activeChunkId,
  onOpenDocument,
  onSelectResult,
  onDeleteDocument,
}: SidebarProps) {
  const searching = search.status !== 'idle'

  return (
    <aside className="sidebar">
      <div className="sidebar-actions">
        <UploadButton uploading={documents.uploading} onUpload={documents.upload} />
        {documents.uploadError ? (
          <p className="results-note error">{documents.uploadError}</p>
        ) : null}
      </div>

      {searching ? (
        <section className="sidebar-section">
          <h2>
            Results
            <button type="button" className="link" onClick={search.clear}>
              clear
            </button>
          </h2>
          <SearchResults
            state={search}
            activeChunkId={activeChunkId}
            onSelect={onSelectResult}
            documentName={(documentId) =>
              documents.byId.get(documentId)?.originalFilename ?? 'Unknown document'
            }
          />
        </section>
      ) : null}

      <section className="sidebar-section sidebar-documents">
        <h2>Documents</h2>
        {documents.error ? <p className="results-note error">{documents.error}</p> : null}
        {documents.loading ? (
          <p className="results-note">Loading…</p>
        ) : (
          <DocumentList
            documents={documents.documents}
            openDocumentId={openDocumentId}
            onOpen={onOpenDocument}
            onDelete={onDeleteDocument}
          />
        )}
      </section>
    </aside>
  )
}
