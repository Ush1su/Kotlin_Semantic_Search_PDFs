package org.ai_processor.vector_storage

import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import org.ai_processor.vector_storage.config.QdrantProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutionException

@Component
class QdrantCollectionInitializer(
    private val qdrantClient: QdrantClient,
    private val properties: QdrantProperties
) : ApplicationRunner {

    private val logger =
        LoggerFactory.getLogger(QdrantCollectionInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        try {
            val collectionExists = qdrantClient
                .collectionExistsAsync(properties.collectionName)
                .get()

            if (collectionExists) {
                logger.info(
                    "Qdrant collection '{}' already exists",
                    properties.collectionName
                )
                return
            }

            val vectorParams = VectorParams.newBuilder()
                .setSize(properties.vectorSize)
                .setDistance(Distance.Cosine)
                .build()

            qdrantClient
                .createCollectionAsync(
                    properties.collectionName,
                    vectorParams
                )
                .get()

            logger.info(
                "Created Qdrant collection '{}' with vector size {}",
                properties.collectionName,
                properties.vectorSize
            )
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()

            throw IllegalStateException(
                "Qdrant collection initialization was interrupted",
                exception
            )
        } catch (exception: ExecutionException) {
            throw IllegalStateException(
                "Failed to initialize Qdrant collection",
                exception.cause ?: exception
            )
        }
    }
}