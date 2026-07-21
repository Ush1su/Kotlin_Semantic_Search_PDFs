package org.ai_processor.document.services

import org.ai_processor.processing.embeddings.EmbeddedChunk
import org.ai_processor.vector_storage.VectorStorage
import org.ai_processor.vector_storage.model.VectorSearchMatch
import java.util.UUID

internal class RecordingVectorStorage : VectorStorage {
    val savedBatches = mutableListOf<List<EmbeddedChunk>>()
    val deletedDocumentIds = mutableListOf<UUID>()
    val searches = mutableListOf<Search>()
    var searchResults = emptyList<VectorSearchMatch>()

    override fun saveAll(chunks: List<EmbeddedChunk>) {
        savedBatches += chunks
    }

    override fun deleteAllByDocumentId(documentId: UUID) {
        deletedDocumentIds += documentId
    }

    override fun search(
        query: String,
        vector: List<Float>,
        limit: Int,
        minimumScore: Float?
    ): List<VectorSearchMatch> {
        searches += Search(vector, limit, minimumScore, query)
        return searchResults
    }

    data class Search(
        val vector: List<Float>,
        val limit: Int,
        val minimumScore: Float?,
        val query: String
    )
}
