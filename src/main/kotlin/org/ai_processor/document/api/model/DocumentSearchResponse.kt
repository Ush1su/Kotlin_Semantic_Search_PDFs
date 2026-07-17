package org.ai_processor.document.api.model

import java.util.UUID

data class DocumentSearchResponse(
    val query: String,
    val results: List<DocumentSearchResultResponse>
)

data class DocumentSearchResultResponse(
    val score: Float,
    val chunk: SearchChunkResponse
)

data class SearchChunkResponse(
    val chunkId: UUID,
    val documentId: UUID,
    val text: String,
)