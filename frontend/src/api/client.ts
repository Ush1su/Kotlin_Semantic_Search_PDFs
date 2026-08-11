import type {
  DocumentResponse,
  DocumentUploadResponse,
  HighlightChunkResponse,
  SearchResponse,
} from './types'

const BASE_URL = '/api'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

/** The backend's exception handler answers with a bare string, not JSON. */
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response

  try {
    response = await fetch(`${BASE_URL}${path}`, init)
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Is the backend running?')
  }

  if (!response.ok) {
    const body = await response.text().catch(() => '')
    throw new ApiError(response.status, body.trim() || `Request failed (${response.status})`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

/**
 * GET /documents returns a Spring Page. Spring Data serializes it either flat or
 * nested under "page" depending on the configured serialization mode, so read
 * whichever we get and hand back just the rows.
 */
interface SpringPage<T> {
  content?: T[]
}

/**
 * The list endpoint stringifies a nullable Instant, so an unprocessed document
 * arrives with processedAt as the literal "null".
 */
function normalizeDocument(document: DocumentResponse): DocumentResponse {
  return {
    ...document,
    processedAt: document.processedAt === 'null' ? null : document.processedAt,
  }
}

export async function listDocuments(size = 100): Promise<DocumentResponse[]> {
  const page = await request<SpringPage<DocumentResponse>>(`/documents?page=0&size=${size}`)
  return (page.content ?? []).map(normalizeDocument)
}

export async function getDocument(documentId: string): Promise<DocumentResponse> {
  return normalizeDocument(await request<DocumentResponse>(`/documents/${documentId}`))
}

export async function uploadDocument(file: File): Promise<DocumentUploadResponse> {
  const body = new FormData()
  body.append('file', file)

  return request<DocumentUploadResponse>('/documents', { method: 'POST', body })
}

export async function deleteDocument(documentId: string): Promise<void> {
  await request<void>(`/documents/${documentId}`, { method: 'DELETE' })
}

export async function getChunkHighlight(chunkId: string): Promise<HighlightChunkResponse> {
  return request<HighlightChunkResponse>(`/documents/chunks/${chunkId}`)
}

export interface SearchParams {
  query: string
  limit: number
  minimumScore: number
  documentId?: string
  signal?: AbortSignal
}

export async function search({
  query,
  limit,
  minimumScore,
  documentId,
  signal,
}: SearchParams): Promise<SearchResponse> {
  const params = new URLSearchParams({
    query,
    limit: String(limit),
    minimumScore: String(minimumScore),
  })

  if (documentId) {
    params.set('documentId', documentId)
  }

  return request<SearchResponse>(`/search?${params.toString()}`, { signal })
}

export function documentFileUrl(documentId: string): string {
  return `${BASE_URL}/documents/${documentId}/file`
}
