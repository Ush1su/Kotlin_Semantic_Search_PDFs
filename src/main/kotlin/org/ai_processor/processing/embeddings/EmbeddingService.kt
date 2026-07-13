package org.ai_processor.processing.embeddings

import org.ai_processor.processing.chunking.PdfChunk
import org.springframework.stereotype.Service

@Service
class EmbeddingService (
    private val embeddingClient: EmbeddingClient,
    private val properties: EmbeddingProperties,
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
            .flatMap(embeddingClient::embed)
    }

    fun embedPdfChunks(chunks: List<PdfChunk>) : List<EmbeddedChunk> {
        val chunkTexts = chunks.map { it.text }
        val embeddings = generateEmbeddings(chunkTexts)
        return chunks.zip(embeddings).map { (chunk, embedding) ->
            EmbeddedChunk(
                chunkId = chunk.id,
                documentId = chunk.documentId,
                vector = embedding,
            )
        }
    }
}