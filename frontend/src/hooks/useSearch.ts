import { useCallback, useEffect, useState } from 'react'
import * as api from '../api/client'
import type { SearchResult } from '../api/types'
import { errorMessage } from '../lib/errors'

// Every keystroke costs an embedding round-trip, so wait for a pause in typing.
const DEBOUNCE_MS = 450
const MIN_QUERY_LENGTH = 2

export type SearchStatus = 'idle' | 'loading' | 'ready' | 'error'

export interface SearchOptions {
  limit: number
  minimumScore: number
  /** Restricts the search to one document; undefined searches everything. */
  documentId?: string
}

export function useSearch({ limit, minimumScore, documentId }: SearchOptions) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [status, setStatus] = useState<SearchStatus>('idle')
  const [error, setError] = useState<string | null>(null)

  const clear = useCallback(() => {
    setQuery('')
    setResults([])
    setStatus('idle')
    setError(null)
  }, [])

  // A scoped search belongs to the document it was opened for.
  useEffect(() => {
    clear()
  }, [documentId, clear])

  useEffect(() => {
    const trimmed = query.trim()

    if (trimmed.length < MIN_QUERY_LENGTH) {
      setResults([])
      setStatus('idle')
      setError(null)
      return
    }

    const controller = new AbortController()
    setStatus('loading')

    const timer = setTimeout(async () => {
      try {
        const response = await api.search({
          query: trimmed,
          limit,
          minimumScore,
          documentId,
          signal: controller.signal,
        })

        setResults(response.results)
        setStatus('ready')
        setError(null)
      } catch (caught) {
        if (controller.signal.aborted) {
          return
        }

        setResults([])
        setError(errorMessage(caught))
        setStatus('error')
      }
    }, DEBOUNCE_MS)

    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [query, limit, minimumScore, documentId])

  return { query, setQuery, results, status, error, clear }
}

export type SearchState = ReturnType<typeof useSearch>
