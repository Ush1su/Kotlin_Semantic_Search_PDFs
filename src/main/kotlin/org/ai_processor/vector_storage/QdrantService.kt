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
import io.qdrant.client.grpc.Points
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.UpdateResult
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
                executeQdrantOperation(
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

    private fun validateChunks(chunks: List<EmbeddedChunk>) {
        chunks.forEach { chunk ->
            if (chunk.vector.size.toLong() != properties.vectorSize) {
                throw VectorStorageException("Vector size mismatch between chunks and Qdrant")
            }
        }
    }

    private fun executeQdrantOperation(
        operationName: String,
        operation: () -> ListenableFuture<UpdateResult>
    ) {
        try {
            operation().get()
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