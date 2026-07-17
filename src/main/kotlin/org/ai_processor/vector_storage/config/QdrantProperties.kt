package org.ai_processor.vector_storage.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.qdrant")
data class QdrantProperties(
    val vectorSize: Long,
    val host: String = "localhost",
    val port: Int = 6334,
    val useTls: Boolean = false,
    val collectionName: String = "chunks",
    val batchSize: Int = 128,
    val timeoutSeconds: Long = 30
)