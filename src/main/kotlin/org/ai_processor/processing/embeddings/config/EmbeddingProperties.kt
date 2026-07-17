package org.ai_processor.processing.embeddings.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.embedding")
data class EmbeddingProperties(
    val baseUrl: String,
    val model: String,
    val batchSize: Int = 32,
    val truncate: Boolean = false,
    val dimensions: Int = 512
)