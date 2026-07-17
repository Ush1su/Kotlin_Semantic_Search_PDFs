package org.ai_processor.vector_storage.config

import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(QdrantProperties::class)
class QdrantConfig {

    @Bean
    fun qdrantClient(
        properties: QdrantProperties
    ): QdrantClient {
        val grpcClient = QdrantGrpcClient
            .newBuilder(
                properties.host,
                properties.port,
                properties.useTls
            )
            .withTimeout(Duration.ofSeconds(properties.timeoutSeconds))
            .build()

        return QdrantClient(grpcClient)
    }
}