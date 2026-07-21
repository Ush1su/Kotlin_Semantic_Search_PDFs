package org.ai_processor.document.services

import org.ai_processor.processing.embeddings.EmbeddingService
import org.ai_processor.vector_storage.VectorStorage
import org.ai_processor.vector_storage.model.VectorSearchMatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DocumentSearchService (
    private val embeddingService: EmbeddingService,
    private val vectorStorage: VectorStorage
){

    private val logger = LoggerFactory.getLogger(DocumentSearchService::class.java)

    fun search(
        query: String,
        limit: Int = 10,
        minimumScore: Float? = 0.5f
    ) : List<VectorSearchMatch> {
        val embedding = embeddingService.embedText(query)
        logger.info("Embedding of $query is $embedding")
        return vectorStorage.search(query, embedding, limit, minimumScore)
    }
}
