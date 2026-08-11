package org.ai_processor.processing.embeddings.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.embedding")
data class EmbeddingProperties(
    val baseUrl: String,
    val model: String,
    val batchSize: Int = 32,
    val maxConcurrentBatches: Int = 2,
    val truncate: Boolean = false,
    val dimensions: Int = 512,
    val connectTimeout: Duration = Duration.ofSeconds(10),
    val readTimeout: Duration = Duration.ofMinutes(2),
)
