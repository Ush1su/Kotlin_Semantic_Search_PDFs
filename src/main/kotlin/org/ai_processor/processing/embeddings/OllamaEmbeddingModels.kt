package org.ai_processor.processing.embeddings

import java.util.UUID

data class OllamaEmbeddingRequest(
    val model: String,
    val input: List<String>,
    val truncate: Boolean,
    val dimensions: Int = 512
)

data class OllamaEmbeddingResponse(
    val embeddings: List<List<Float>>,
)

data class EmbeddedChunk(
    val chunkId: UUID,
    val text: String,
    val documentId: UUID,
    val vector: List<Float>,
)