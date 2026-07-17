package org.ai_processor.vector_storage

import com.google.common.util.concurrent.ListenableFuture
import io.qdrant.client.ConditionFactory.matchKeyword
import io.qdrant.client.QdrantClient
import org.ai_processor.processing.embeddings.EmbeddedChunk
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorsFactory.vectors
import org.springframework.stereotype.Service
import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.grpc.Common.Filter
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.UpdateResult
import org.ai_processor.vector_storage.config.QdrantProperties
import org.ai_processor.vector_storage.model.VectorSearchMatch
import io.qdrant.client.grpc.Points.ScoredPoint
import io.qdrant.client.grpc.Points.SearchPoints
import io.qdrant.client.grpc.Points.WithPayloadSelector
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

@Service
class QdrantService(
    private val qdrantClient: QdrantClient,
    private val properties: QdrantProperties,
) : VectorStorage {
    override fun saveAll(chunks: List<EmbeddedChunk>) {
        if (chunks.isEmpty()) return
        validateChunks(chunks)

        chunks
            .chunked(properties.batchSize)
            .forEach { batch ->
                executeQdrantOperation<UpdateResult>(
                    operationName = "save ${batch.size} chunks"
                ) {
                    qdrantClient.upsertAsync(
                        properties.collectionName,
                        batch.map(::toPoint)
                    )
                }
            }
    }

    override fun deleteAllByDocumentId(documentId: UUID) {
        val filter = Filter.newBuilder()
            .addMust(
                matchKeyword(
                    DOCUMENT_ID_PAYLOAD,
                    documentId.toString()
                )
            )
            .build()

        executeQdrantOperation(
            operationName = "delete chunks for document id $documentId",
        ) {
            qdrantClient.deleteAsync(
                properties.collectionName,
                filter
            )
        }
    }

    override fun searchByVector(
        vector: List<Float>,
        limit: Int,
        minimumScore: Float?
    ): List<VectorSearchMatch> {
        validateSearchArguments(vector, limit, minimumScore)
        val request = SearchPoints.newBuilder()
            .setCollectionName(properties.collectionName)
            .addAllVector(vector)
            .setLimit(limit.toLong())
            .setWithPayload(
                WithPayloadSelector.newBuilder()
                    .setEnable(true)
                    .build()
            )
            .apply {
                minimumScore?.let(::setScoreThreshold)
            }
            .build()
        return executeQdrantOperation(
            operationName = "search for $limit nearest chunks"
        ) {
            qdrantClient.searchAsync(request)
        }.map(::toVectorSearchMatch)
    }

    private fun toVectorSearchMatch(
        point: ScoredPoint
    ): VectorSearchMatch {
        val chunkId = point.id.uuid
            .takeIf {it.isNotBlank()}
            ?.let(UUID::fromString)
            ?: throw VectorStorageSearchException("Qdrant search result has no valid chunk UUID")

        val documentId = UUID.fromString(
            requiredPayloadString(point, DOCUMENT_ID_PAYLOAD)
        )

        return VectorSearchMatch(
            chunkId = chunkId,
            documentId = documentId,
            text = requiredPayloadString(point, TEXT_PAYLOAD),
            score = point.score
        )
    }

    private fun requiredPayloadString(
        point: ScoredPoint,
        key: String
    ): String {
        return point.payloadMap[key]
            ?.stringValue
            ?.takeIf { it.isNotBlank() }
            ?: throw VectorStorageSearchException(
                "Qdrant search result is missing '$key' payload"
            )
    }

    private fun validateSearchArguments(
        vector: List<Float>,
        limit: Int,
        minimumScore: Float?
    ) {
        if (vector.size.toLong() != properties.vectorSize) {
            throw VectorStorageException(
                "Search vector size ${vector.size} does not match. Qdrant vector size ${properties.vectorSize}"
            )
        }

        require(limit > 0) {
            "Search limit must be greater than zero"
        }

        require(minimumScore == null || minimumScore.isFinite()) {
            "Minimum score must be a finite number"
        }
    }

    private fun validateChunks(chunks: List<EmbeddedChunk>) {
        chunks.forEach { chunk ->
            if (chunk.vector.size.toLong() != properties.vectorSize) {
                throw VectorStorageException("Vector size mismatch between chunks and Qdrant")
            }
        }
    }

    private fun <T> executeQdrantOperation(
        operationName: String,
        operation: () -> ListenableFuture<T>
    ): T {
        try {
            return operation().get()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()

            throw VectorStorageInterruptedException(
                message = "Qdrant operation was interrupted: $operationName",
                cause = exception
            )
        } catch (exception: ExecutionException) {
            throw VectorStorageException(
                message = "Qdrant operation failed: $operationName",
                cause = exception.cause ?: exception
            )
        } catch (exception: CancellationException) {
            throw VectorStorageException(
                message = "Qdrant operation was cancelled: $operationName",
                cause = exception
            )
        }
    }

    private fun toPoint(chunk: EmbeddedChunk): PointStruct {
        return PointStruct.newBuilder()
            .setId(id(chunk.chunkId))
            .setVectors(vectors(chunk.vector))
            .putAllPayload(
                mapOf(
                    DOCUMENT_ID_PAYLOAD to value(chunk.documentId.toString()),
                    TEXT_PAYLOAD to value(chunk.text)
                )
            )
            .build()
    }
    private companion object {
        const val DOCUMENT_ID_PAYLOAD = "document_id"
        const val TEXT_PAYLOAD = "text"
    }
}