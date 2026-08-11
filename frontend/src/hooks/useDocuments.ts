import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import * as api from '../api/client'
import type { DocumentResponse } from '../api/types'
import { errorMessage } from '../lib/errors'

const POLL_INTERVAL_MS = 2000

function isPending(document: DocumentResponse): boolean {
  return document.status === 'UPLOADED' || document.status === 'PROCESSING'
}

/**
 * Owns the document list. Uploads are processed asynchronously by the backend, so
 * the list re-polls while anything is still UPLOADED or PROCESSING.
 */
export function useDocuments() {
  const [documents, setDocuments] = useState<DocumentResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  // Documents referenced by a search hit but outside the fetched page.
  const [resolved, setResolved] = useState<Record<string, DocumentResponse>>({})
  const requestedRef = useRef(new Set<string>())

  const refresh = useCallback(async () => {
    try {
      setDocuments(await api.listDocuments())
      setError(null)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const hasPending = documents.some(isPending)

  useEffect(() => {
    if (!hasPending) {
      return
    }

    const timer = setInterval(() => void refresh(), POLL_INTERVAL_MS)
    return () => clearInterval(timer)
  }, [hasPending, refresh])

  const upload = useCallback(
    async (files: File[]) => {
      setUploading(true)
      setUploadError(null)

      try {
        for (const file of files) {
          await api.uploadDocument(file)
        }
      } catch (caught) {
        setUploadError(errorMessage(caught))
      } finally {
        setUploading(false)
        await refresh()
      }
    },
    [refresh],
  )

  const remove = useCallback(
    async (documentId: string) => {
      try {
        await api.deleteDocument(documentId)
      } catch (caught) {
        setError(errorMessage(caught))
      } finally {
        await refresh()
      }
    },
    [refresh],
  )

  /** Pulls in a document the list does not cover, so search hits can be labelled. */
  const ensureDocument = useCallback((documentId: string) => {
    if (requestedRef.current.has(documentId)) {
      return
    }
    requestedRef.current.add(documentId)

    api
      .getDocument(documentId)
      .then((document) => setResolved((current) => ({ ...current, [documentId]: document })))
      .catch(() => requestedRef.current.delete(documentId))
  }, [])

  const byId = useMemo(() => {
    const map = new Map<string, DocumentResponse>()
    for (const document of Object.values(resolved)) {
      map.set(document.id, document)
    }
    for (const document of documents) {
      map.set(document.id, document)
    }
    return map
  }, [documents, resolved])

  return {
    documents,
    byId,
    loading,
    error,
    uploading,
    uploadError,
    upload,
    remove,
    ensureDocument,
    refresh,
  }
}

export type DocumentsState = ReturnType<typeof useDocuments>
