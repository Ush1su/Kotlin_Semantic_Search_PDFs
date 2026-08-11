import type { SearchResult } from '../api/types'
import type { SearchState } from '../hooks/useSearch'

interface SearchResultsProps {
  state: SearchState
  /** Chunk currently highlighted in the viewer, if any. */
  activeChunkId: string | null
  onSelect: (result: SearchResult) => void
  /** Global results name their document; in-document results do not need to. */
  documentName?: (documentId: string) => string
}

const SNIPPET_LENGTH = 220

function snippet(text: string): string {
  const collapsed = text.replace(/\s+/g, ' ').trim()

  return collapsed.length > SNIPPET_LENGTH
    ? `${collapsed.slice(0, SNIPPET_LENGTH)}…`
    : collapsed
}

export function SearchResults({
  state,
  activeChunkId,
  onSelect,
  documentName,
}: SearchResultsProps) {
  if (state.status === 'idle') {
    return null
  }

  if (state.status === 'loading') {
    return <p className="results-note">Searching…</p>
  }

  if (state.status === 'error') {
    return <p className="results-note error">{state.error}</p>
  }

  if (state.results.length === 0) {
    return <p className="results-note">No passage matched closely enough.</p>
  }

  return (
    <ul className="results">
      {state.results.map((result) => (
        <li key={result.chunk.chunkId}>
          <button
            type="button"
            className={`result${result.chunk.chunkId === activeChunkId ? ' is-active' : ''}`}
            onClick={() => onSelect(result)}
          >
            <span className="result-head">
              <span className="result-score">{result.score.toFixed(2)}</span>
              {documentName ? (
                <span className="result-document">{documentName(result.chunk.documentId)}</span>
              ) : null}
            </span>
            <span className="result-text">{snippet(result.chunk.text)}</span>
          </button>
        </li>
      ))}
    </ul>
  )
}
