import type { DocumentResponse } from '../api/types'

interface DocumentListProps {
  documents: DocumentResponse[]
  openDocumentId: string | null
  onOpen: (documentId: string) => void
  onDelete: (document: DocumentResponse) => void
}

function formatSize(bytes: number): string {
  const megabytes = bytes / 1024 / 1024

  return megabytes >= 1 ? `${megabytes.toFixed(1)} MB` : `${Math.round(bytes / 1024)} KB`
}

export function DocumentList({
  documents,
  openDocumentId,
  onOpen,
  onDelete,
}: DocumentListProps) {
  if (documents.length === 0) {
    return <p className="results-note">No documents yet. Upload a PDF to get started.</p>
  }

  return (
    <ul className="documents">
      {documents.map((document) => (
        <li key={document.id}>
          <button
            type="button"
            className={`document${document.id === openDocumentId ? ' is-active' : ''}`}
            onClick={() => onOpen(document.id)}
          >
            <span className="document-name" title={document.originalFilename}>
              {document.originalFilename}
            </span>
            <span className="document-meta">
              <span className={`status status-${document.status.toLowerCase()}`}>
                {document.status.toLowerCase()}
              </span>
              <span>{formatSize(document.fileSizeBytes)}</span>
            </span>
            {document.status === 'FAILED' && document.errorMessage ? (
              <span className="document-error">{document.errorMessage}</span>
            ) : null}
          </button>

          <button
            type="button"
            className="document-delete"
            title="Delete document"
            aria-label={`Delete ${document.originalFilename}`}
            onClick={() => onDelete(document)}
          >
            ×
          </button>
        </li>
      ))}
    </ul>
  )
}
