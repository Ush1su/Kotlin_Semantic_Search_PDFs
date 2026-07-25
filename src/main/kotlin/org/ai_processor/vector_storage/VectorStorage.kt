package org.ai_processor.vector_storage

import org.ai_processor.processing.embeddings.EmbeddedChunk
import org.ai_processor.vector_storage.model.VectorSearchMatch
import java.util.UUID

interface VectorStorage {
    fun saveAll(chunks: List<EmbeddedChunk>)
    fun deleteAllByDocumentIdAndUserId(userId: UUID, documentId: UUID)
    fun search(
        userId: UUID,
        query: String,
        vector: List<Float>,
        limit: Int,
        minimumScore: Float? = null
    ): List<VectorSearchMatch>
}
