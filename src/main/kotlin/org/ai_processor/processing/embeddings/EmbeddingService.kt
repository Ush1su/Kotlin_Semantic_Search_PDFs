package org.ai_processor.processing.embeddings

import org.ai_processor.processing.chunking.PdfChunk
import org.ai_processor.processing.embeddings.config.EmbeddingProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

@Service
class EmbeddingService (
    private val embeddingClient: EmbeddingClient,
    private val properties: EmbeddingProperties,
    @param:Qualifier("embeddingBatchExecutor")
    private val embeddingBatchExecutor: Executor = Executor { task -> task.run() },
) {

    private fun generateEmbeddings(
        texts: List<String>,
    ): List<List<Float>> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        require(texts.none(String::isBlank)) {
            "Texts must not contain blank values"
        }

        return texts
            .chunked(properties.batchSize)
            .chunked(properties.maxConcurrentBatches)
            .flatMap { batchGroup ->
                batchGroup
                    .map { batch ->
                        CompletableFuture.supplyAsync(
                            { embeddingClient.embed(batch) },
                            embeddingBatchExecutor
                        )
                    }
                    .flatMap(::getEmbeddingBatch)
            }
    }

    private fun getEmbeddingBatch(
        future: CompletableFuture<List<List<Float>>>
    ): List<List<Float>> {
        try {
            return future.get()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw EmbeddingException(
                message = "Embedding batch generation was interrupted",
                cause = exception
            )
        } catch (exception: ExecutionException) {
            throw EmbeddingException(
                message = "Failed to generate embedding batch",
                cause = exception.cause ?: exception
            )
        }
    }

    fun embedPdfChunks(chunks: List<PdfChunk>) : List<EmbeddedChunk> {
        val chunkTexts = chunks.map { it.text }
        val embeddings = generateEmbeddings(chunkTexts)
        return chunks.zip(embeddings).map { (chunk, embedding) ->
            EmbeddedChunk(
                chunkId = chunk.id,
                text = chunk.text,
                documentId = chunk.documentId,
                vector = embedding,
            )
        }
    }
    fun embedText(text: String) : List<Float> {
        return embeddingClient.embed(listOf(text)).first()
    }
}
