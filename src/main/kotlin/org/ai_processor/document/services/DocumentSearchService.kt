package org.ai_processor.document.services

import org.ai_processor.processing.embeddings.EmbeddingService
import org.ai_processor.vector_storage.VectorStorage
import org.ai_processor.vector_storage.model.VectorSearchMatch
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

@Service
class DocumentSearchService (
    private val embeddingService: EmbeddingService,
    private val vectorStorage: VectorStorage,
    @param:Qualifier("searchExecutor")
    private val searchExecutor: Executor = Executor { task -> task.run() }
){

    private val logger = LoggerFactory.getLogger(DocumentSearchService::class.java)

    fun search(
        userId: UUID,
        query: String,
        limit: Int = 10,
        minimumScore: Float? = 0.5f
    ) : List<VectorSearchMatch> {
        val future = CompletableFuture.supplyAsync(
            {
                searchBlocking(
                    userId = userId,
                    query = query,
                    limit = limit,
                    minimumScore = minimumScore
                )
            },
            searchExecutor
        )

        return getSearchResults(future)
    }

    private fun searchBlocking(
        userId: UUID,
        query: String,
        limit: Int,
        minimumScore: Float?
    ): List<VectorSearchMatch> {
        val embedding = embeddingService.embedText(query)
        logger.info("Embedding of $query is $embedding")
        return vectorStorage.search(userId, query, embedding, limit, minimumScore)
    }

    private fun getSearchResults(
        future: CompletableFuture<List<VectorSearchMatch>>
    ): List<VectorSearchMatch> {
        try {
            return future.get()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Search was interrupted", exception)
        } catch (exception: ExecutionException) {
            throw exception.cause ?: exception
        }
    }
}
