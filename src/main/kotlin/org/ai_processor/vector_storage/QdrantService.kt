package org.ai_processor.vector_storage

import com.google.common.util.concurrent.ListenableFuture
import io.qdrant.client.ConditionFactory.matchKeyword
import io.qdrant.client.QdrantClient
import io.qdrant.client.QueryFactory.fusion
import io.qdrant.client.QueryFactory.nearest
import org.ai_processor.processing.embeddings.EmbeddedChunk
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorFactory.vector
import io.qdrant.client.VectorsFactory.namedVectors
import org.springframework.stereotype.Service
import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.grpc.Common.Filter
import io.qdrant.client.grpc.Points.Fusion
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.PrefetchQuery
import io.qdrant.client.grpc.Points.Query
import io.qdrant.client.grpc.Points.QueryPoints
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
    private val bm25DocumentFactory: QdrantBm25DocumentFactory
) : VectorStorage {
    override fun saveAll(chunks: List<EmbeddedChunk>) {
        if (chunks.isEmpty()) return
        validateChunks(chunks)

        chunks
            .chunked(properties.batchSize)
            .chunked(properties.maxConcurrentUpsertBatches)
            .forEach { batchGroup ->
                val upserts = batchGroup.map { batch ->
                    batch to qdrantClient.upsertAsync(
                        properties.collectionName,
                        batch.map(::toPoint)
                    )
                }
                upserts.forEach { (batch, future) ->
                    executeQdrantOperation<UpdateResult>(
                        operationName = "save ${batch.size} chunks"
                    ) {
                        future
                    }
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

    override fun search(
        query: String,
        vector: List<Float>,
        limit: Int,
        minimumScore: Float?
    ): List<VectorSearchMatch> {
        validateSearchArguments(vector, limit, minimumScore)

        val densePrefetch = prefetch(
            query = nearest(vector),
            using = properties.denseVectorName,
            limit = hybridPrefetchLimit(limit),
            minimumScore = minimumScore
        )
        val lexicalPrefetch = prefetch(
            query = nearest(bm25DocumentFactory.document(query)),
            using = properties.bm25VectorName,
            limit = hybridPrefetchLimit(limit),
            minimumScore = null
        )

        val request = QueryPoints.newBuilder()
            .setCollectionName(properties.collectionName)
            .addPrefetch(densePrefetch)
            .addPrefetch(lexicalPrefetch)
            .setQuery(fusion(Fusion.RRF))
            .setLimit(limit.toLong())
            .setWithPayload(enabledPayloadSelector())
            .build()

        return executeQdrantOperation(
            operationName = "hybrid search for $limit nearest chunks"
        ) {
            qdrantClient.queryAsync(request)
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

        if (limit <= 0) {
            throw VectorStorageSearchException("Search limit must be greater than zero")
        }

        if (minimumScore != null && !minimumScore.isFinite()) {
            throw VectorStorageSearchException("Minimum score must be a finite number")
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
        val pointVectors = mutableMapOf(
            properties.denseVectorName to vector(chunk.vector)
        )
        pointVectors[properties.bm25VectorName] = vector(
            bm25DocumentFactory.document(chunk.text)
        )

        return PointStruct.newBuilder()
            .setId(id(chunk.chunkId))
            .setVectors(namedVectors(pointVectors))
            .putAllPayload(
                mapOf(
                    DOCUMENT_ID_PAYLOAD to value(chunk.documentId.toString()),
                    TEXT_PAYLOAD to value(chunk.text)
                )
            )
            .build()
    }

    private fun enabledPayloadSelector(): WithPayloadSelector {
        return WithPayloadSelector.newBuilder()
            .setEnable(true)
            .build()
    }

    private fun prefetch(
        query: Query,
        using: String,
        limit: Int,
        minimumScore: Float?
    ): PrefetchQuery {
        return PrefetchQuery.newBuilder()
            .setQuery(query)
            .setUsing(using)
            .setLimit(limit.toLong())
            .apply {
                minimumScore?.let(::setScoreThreshold)
            }
            .build()
    }

    private fun hybridPrefetchLimit(limit: Int): Int {
        val multipliedLimit = limit * properties.hybridPrefetchMultiplier
        return maxOf(limit, minOf(multipliedLimit, properties.hybridMaxPrefetchLimit))
    }

    private companion object {
        const val DOCUMENT_ID_PAYLOAD = "document_id"
        const val TEXT_PAYLOAD = "text"
    }
}
