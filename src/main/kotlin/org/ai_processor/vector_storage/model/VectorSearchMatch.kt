package org.ai_processor.vector_storage.model

import java.util.UUID

data class VectorSearchMatch(
    val chunkId: UUID,
    val documentId: UUID,
    val text: String,
    val score: Float
)