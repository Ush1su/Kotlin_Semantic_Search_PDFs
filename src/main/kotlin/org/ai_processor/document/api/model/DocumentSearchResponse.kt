package org.ai_processor.document.api.model

import org.ai_processor.processing.chunking.HighlightRect
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

data class HighlightChunkResponse(
    val chunkId: UUID,
    val documentId: UUID,
    val text: String,
    val highlight: List<HighlightRect>
)