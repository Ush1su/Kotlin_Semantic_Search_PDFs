package org.ai_processor.vector_storage.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.qdrant")
data class QdrantProperties(
    val vectorSize: Long,
    val host: String = "localhost",
    val port: Int = 6334,
    val useTls: Boolean = false,
    val collectionName: String = "chunks",
    val denseVectorName: String = "dense",
    val bm25VectorName: String = "bm25",
    val hybridPrefetchMultiplier: Int = 4,
    val hybridMaxPrefetchLimit: Int = 100,
    val bm25TokenMinLength: Int = 2,
    val batchSize: Int = 128,
    val timeoutSeconds: Long = 30
) {
    init {
        require(vectorSize > 0) {
            "Qdrant vector size must be greater than zero"
        }
        require(denseVectorName.isNotBlank()) {
            "Qdrant dense vector name must not be blank"
        }
        require(bm25VectorName.isNotBlank()) {
            "Qdrant BM25 vector name must not be blank"
        }
        require(hybridPrefetchMultiplier > 0) {
            "Qdrant hybrid prefetch multiplier must be greater than zero"
        }
        require(hybridMaxPrefetchLimit > 0) {
            "Qdrant hybrid max prefetch limit must be greater than zero"
        }
        require(bm25TokenMinLength > 0) {
            "Qdrant BM25 token min length must be greater than zero"
        }
    }
}
