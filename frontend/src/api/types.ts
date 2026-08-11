/** Mirrors of the Kotlin DTOs in org.ai_processor.document.api.model. */

export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'

export interface DocumentResponse {
  id: string
  originalFilename: string
  contentType: string
  fileSizeBytes: number
  status: DocumentStatus
  errorMessage: string | null
  createdAt: string
  processedAt: string | null
}

export interface DocumentUploadResponse {
  documentId: string | null
  status: string
}

export interface BoundingBox {
  left: number
  bottom: number
  right: number
  top: number
}

export interface HighlightRect {
  pageNumber: number
  bbox: BoundingBox
}

export interface HighlightChunkResponse {
  chunkId: string
  documentId: string
  text: string
  highlight: HighlightRect[]
}

export interface SearchChunk {
  chunkId: string
  documentId: string
  text: string
}

export interface SearchResult {
  score: number
  chunk: SearchChunk
}

export interface SearchResponse {
  query: string
  results: SearchResult[]
}
