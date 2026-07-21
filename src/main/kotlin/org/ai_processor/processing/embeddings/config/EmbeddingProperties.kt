package org.ai_processor.processing.embeddings.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.embedding")
data class EmbeddingProperties(
    val baseUrl: String,
    val model: String,
    val batchSize: Int = 32,
    val maxConcurrentBatches: Int = 2,
    val truncate: Boolean = false,
    val dimensions: Int = 512
) {
    init {
        require(batchSize > 0) {
            "Embedding batch size must be greater than zero"
        }
        require(maxConcurrentBatches > 0) {
            "Embedding max concurrent batches must be greater than zero"
        }
        require(dimensions > 0) {
            "Embedding dimensions must be greater than zero"
        }
    }
}
