package org.ai_processor.processing.embeddings

import org.ai_processor.processing.chunking.PdfChunk
import org.ai_processor.processing.embeddings.config.EmbeddingProperties
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EmbeddingServiceTest {
    @Test
    fun `embedPdfChunks returns empty list without calling client for empty chunks`() {
        val client = RecordingEmbeddingClient(emptyMap())
        val service = embeddingService(client)

        val embeddedChunks = service.embedPdfChunks(emptyList())

        assertTrue(embeddedChunks.isEmpty())
        assertTrue(client.calls.isEmpty())
    }

    @Test
    fun `embedPdfChunks batches texts and preserves chunk metadata`() {
        val documentId = UUID.randomUUID()
        val chunks = listOf(
            pdfChunk(documentId = documentId, text = "alpha", chunkIndex = 1),
            pdfChunk(documentId = documentId, text = "beta", chunkIndex = 2),
            pdfChunk(documentId = documentId, text = "gamma", chunkIndex = 3)
        )
        val client = RecordingEmbeddingClient(
            vectorsByText = mapOf(
                "alpha" to listOf(1f, 0f),
                "beta" to listOf(0f, 1f),
                "gamma" to listOf(0.5f, 0.5f)
            )
        )
        val service = embeddingService(client, batchSize = 2)

        val embeddedChunks = service.embedPdfChunks(chunks)

        assertEquals(listOf(listOf("alpha", "beta"), listOf("gamma")), client.calls)
        assertEquals(chunks.map { it.id }, embeddedChunks.map { it.chunkId })
        assertEquals(chunks.map { it.documentId }, embeddedChunks.map { it.documentId })
        assertEquals(chunks.map { it.text }, embeddedChunks.map { it.text })
        assertEquals(
            listOf(listOf(1f, 0f), listOf(0f, 1f), listOf(0.5f, 0.5f)),
            embeddedChunks.map { it.vector }
        )
    }

    @Test
    fun `embedPdfChunks rejects blank chunk text before calling client`() {
        val client = RecordingEmbeddingClient(emptyMap())
        val service = embeddingService(client)
        val chunks = listOf(pdfChunk(text = "   "))

        assertFailsWith<IllegalArgumentException> {
            service.embedPdfChunks(chunks)
        }

        assertTrue(client.calls.isEmpty())
    }

    @Test
    fun `embedText returns first embedding for query text`() {
        val client = RecordingEmbeddingClient(
            vectorsByText = mapOf("query" to listOf(0.25f, 0.75f))
        )
        val service = embeddingService(client)

        val embedding = service.embedText("query")

        assertEquals(listOf(0.25f, 0.75f), embedding)
        assertEquals(listOf(listOf("query")), client.calls)
    }

    private fun embeddingService(
        client: RecordingEmbeddingClient,
        batchSize: Int = 32
    ): EmbeddingService {
        return EmbeddingService(
            embeddingClient = client,
            properties = EmbeddingProperties(
                baseUrl = "http://localhost:11434",
                model = "test-embedding",
                batchSize = batchSize
            )
        )
    }

    private fun pdfChunk(
        id: UUID = UUID.randomUUID(),
        documentId: UUID = UUID.randomUUID(),
        text: String,
        chunkIndex: Int = 1
    ): PdfChunk {
        return PdfChunk(
            id = id,
            documentId = documentId,
            chunkIndex = chunkIndex,
            text = text,
            pageStart = 1,
            pageEnd = 1,
            highlightRects = emptyList()
        )
    }

    private class RecordingEmbeddingClient(
        private val vectorsByText: Map<String, List<Float>>
    ) : EmbeddingClient {
        val calls = mutableListOf<List<String>>()

        override fun embed(texts: List<String>): List<List<Float>> {
            calls += texts
            return texts.map { text ->
                vectorsByText[text] ?: error("No vector configured for text: $text")
            }
        }
    }
}
