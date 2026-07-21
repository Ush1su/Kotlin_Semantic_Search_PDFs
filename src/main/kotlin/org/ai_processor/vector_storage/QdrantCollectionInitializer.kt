package org.ai_processor.vector_storage

import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Collections.CreateCollection
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.Modifier
import io.qdrant.client.grpc.Collections.SparseVectorConfig
import io.qdrant.client.grpc.Collections.SparseVectorParams
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.Collections.VectorParamsMap
import io.qdrant.client.grpc.Collections.VectorsConfig
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
                return
            }

            val vectorParams = VectorParams.newBuilder()
                .setSize(properties.vectorSize)
                .setDistance(Distance.Cosine)
                .build()

            qdrantClient
                .createCollectionAsync(
                    CreateCollection.newBuilder()
                        .setCollectionName(properties.collectionName)
                        .setVectorsConfig(
                            VectorsConfig.newBuilder()
                                .setParamsMap(
                                    VectorParamsMap.newBuilder()
                                        .putMap(properties.denseVectorName, vectorParams)
                                )
                        )
                        .setSparseVectorsConfig(sparseVectorConfig())
                        .build()
                )
                .get()

            logger.info(
                "Created Qdrant collection '{}' with dense vector size {} and BM25 sparse vector",
                properties.collectionName,
                properties.vectorSize
            )
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()

            throw VectorStorageInitializationException(
                "Qdrant collection initialization was interrupted",
                exception
            )
        } catch (exception: ExecutionException) {
            throw VectorStorageInitializationException(
                "Failed to initialize Qdrant collection",
                exception.cause ?: exception
            )
        }
    }

    private fun sparseVectorConfig(): SparseVectorConfig {
        return SparseVectorConfig.newBuilder()
            .putMap(
                properties.bm25VectorName,
                SparseVectorParams.newBuilder()
                    .setModifier(Modifier.Idf)
                    .build()
            )
            .build()
    }
}
