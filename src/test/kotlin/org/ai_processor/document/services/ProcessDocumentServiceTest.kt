package org.ai_processor.document.services

import org.ai_processor.document.persistence.DocumentPersistenceService
import org.ai_processor.document.persistence.model.DocumentChunkEntity
import org.ai_processor.document.persistence.repository.DocumentChunkRepository
import org.ai_processor.document.persistence.repository.DocumentRepository
import org.ai_processor.processing.chunking.Chunker
import org.ai_processor.processing.chunking.HighlightRect
import org.ai_processor.processing.chunking.PdfChunk
import org.ai_processor.processing.embeddings.EmbeddedChunk
import org.ai_processor.processing.embeddings.EmbeddingClient
import org.ai_processor.processing.embeddings.EmbeddingService
import org.ai_processor.processing.embeddings.config.EmbeddingProperties
import org.ai_processor.processing.pdfreader.BoundingBox
import org.ai_processor.processing.pdfreader.ParsedPDF
import org.ai_processor.processing.pdfreader.ParsedPage
import org.ai_processor.processing.pdfreader.PDFParser
import org.ai_processor.vector_storage.VectorStorage
import org.ai_processor.vector_storage.model.VectorSearchMatch
import org.mockito.Mockito.mock
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ProcessDocumentServiceTest {
    @Test
    fun `process runs parsing chunking embedding vector save and marks document ready`() {
        val events = mutableListOf<String>()
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val storagePath = "/tmp/document.pdf"
        val parsedPDF = ParsedPDF(
            documentId = documentId,
            pages = listOf(ParsedPage(1, 612f, 792f, emptyList()))
        )
        val chunks = listOf(pdfChunk(documentId, chunkIndex = 1))
        val embeddedChunks = listOf(
            EmbeddedChunk(
                userId = userId,
                chunkId = chunks.single().id,
                documentId = documentId,
                text = chunks.single().text,
                vector = listOf(0.1f, 0.2f)
            )
        )
        val persistence = RecordingDocumentPersistenceService(events)
        val vectorStorage = RecordingVectorStorage(events)
        val pdfParser = RecordingPDFParser(events, parsedPDF)
        val service = processDocumentService(
            persistence = persistence,
            embeddingService = RecordingEmbeddingService(events, embeddedChunks),
            vectorStorage = vectorStorage,
            pdfParser = pdfParser,
            chunker = RecordingChunker(events, chunks)
        )

        service.process(documentId, storagePath, userId)

        assertEquals(
            listOf(
                "markProcessing",
                "parse",
                "chunk",
                "saveChunks",
                "embed",
                "saveVectors",
                "markReady"
            ),
            events
        )
        assertEquals(documentId, persistence.processingDocumentIds.single())
        assertEquals(documentId, persistence.readyDocumentIds.single())
        assertEquals(Path.of(storagePath), pdfParser.parsedPath)
        assertEquals(documentId, pdfParser.parsedDocumentId)
        assertEquals(listOf(embeddedChunks), vectorStorage.savedBatches)

        val savedChunk = persistence.savedChunks.single().single()
        assertEquals(chunks.single().id, savedChunk.id)
        assertEquals(documentId, savedChunk.documentId)
        assertEquals(chunks.single().chunkIndex, savedChunk.chunkIndex)
        assertEquals(chunks.single().text, savedChunk.text)
        assertEquals(chunks.single().pageStart, savedChunk.pageStart)
        assertEquals(chunks.single().pageEnd, savedChunk.pageEnd)
        assertEquals(chunks.single().highlightRects, savedChunk.highlightRects)
    }

    @Test
    fun `process marks document failed when PDF parsing fails`() {
        val events = mutableListOf<String>()
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val storagePath = "/tmp/broken.pdf"
        val failure = RuntimeException("cannot parse")
        val persistence = RecordingDocumentPersistenceService(events)
        val vectorStorage = RecordingVectorStorage(events)
        val service = processDocumentService(
            persistence = persistence,
            embeddingService = RecordingEmbeddingService(events, emptyList()),
            vectorStorage = vectorStorage,
            pdfParser = RecordingPDFParser(events, failure = failure),
            chunker = RecordingChunker(events, emptyList())
        )

        val thrown = assertFailsWith<RuntimeException> {
            service.process(documentId, storagePath, userId)
        }

        assertSame(failure, thrown)
        assertEquals(listOf("markProcessing", "parse", "markFailed"), events)
        assertEquals(listOf(documentId), persistence.processingDocumentIds)
        assertEquals(
            listOf<Pair<UUID, String?>>(documentId to "cannot parse"),
            persistence.failedDocuments
        )
        assertEquals(0, persistence.savedChunks.size)
        assertEquals(0, vectorStorage.savedBatches.size)
    }

    @Test
    fun `process marks document failed when embedding fails`() {
        val events = mutableListOf<String>()
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val parsedPDF = ParsedPDF(documentId, emptyList())
        val chunks = listOf(pdfChunk(documentId, chunkIndex = 1))
        val failure = RuntimeException("embedding unavailable")
        val persistence = RecordingDocumentPersistenceService(events)
        val vectorStorage = RecordingVectorStorage(events)
        val service = processDocumentService(
            persistence = persistence,
            embeddingService = RecordingEmbeddingService(events, failure = failure),
            vectorStorage = vectorStorage,
            pdfParser = RecordingPDFParser(events, parsedPDF),
            chunker = RecordingChunker(events, chunks)
        )

        val thrown = assertFailsWith<RuntimeException> {
            service.process(documentId, "/tmp/document.pdf", userId)
        }

        assertSame(failure, thrown)
        assertEquals(
            listOf("markProcessing", "parse", "chunk", "saveChunks", "embed", "markFailed"),
            events
        )
        assertEquals(
            listOf<Pair<UUID, String?>>(documentId to "embedding unavailable"),
            persistence.failedDocuments
        )
        assertEquals(0, vectorStorage.savedBatches.size)
    }

    @Test
    fun `process marks document failed when vector storage save fails`() {
        val events = mutableListOf<String>()
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val parsedPDF = ParsedPDF(documentId, emptyList())
        val chunks = listOf(pdfChunk(documentId, chunkIndex = 1))
        val embeddedChunks = listOf(
            EmbeddedChunk(
                userId = userId,
                chunkId = chunks.single().id,
                documentId = documentId,
                text = chunks.single().text,
                vector = listOf(0.1f, 0.2f)
            )
        )
        val failure = RuntimeException("qdrant unavailable")
        val persistence = RecordingDocumentPersistenceService(events)
        val service = processDocumentService(
            persistence = persistence,
            embeddingService = RecordingEmbeddingService(events, embeddedChunks),
            vectorStorage = RecordingVectorStorage(events, failure = failure),
            pdfParser = RecordingPDFParser(events, parsedPDF),
            chunker = RecordingChunker(events, chunks)
        )

        val thrown = assertFailsWith<RuntimeException> {
            service.process(documentId, "/tmp/document.pdf", userId)
        }

        assertSame(failure, thrown)
        assertEquals(
            listOf(
                "markProcessing",
                "parse",
                "chunk",
                "saveChunks",
                "embed",
                "saveVectors",
                "markFailed"
            ),
            events
        )
        assertEquals(
            listOf<Pair<UUID, String?>>(documentId to "qdrant unavailable"),
            persistence.failedDocuments
        )
    }

    private fun processDocumentService(
        persistence: RecordingDocumentPersistenceService,
        embeddingService: EmbeddingService,
        vectorStorage: VectorStorage,
        pdfParser: PDFParser,
        chunker: Chunker
    ): ProcessDocumentService {
        return ProcessDocumentService(
            documentPersistenceService = persistence,
            embeddingService = embeddingService,
            vectorStorage = vectorStorage,
            pdfParser = pdfParser,
            chunker = chunker
        )
    }

    private fun pdfChunk(
        documentId: UUID,
        chunkIndex: Int
    ): PdfChunk {
        return PdfChunk(
            id = UUID.randomUUID(),
            documentId = documentId,
            chunkIndex = chunkIndex,
            text = "chunk text $chunkIndex",
            pageStart = 1,
            pageEnd = 1,
            highlightRects = listOf(
                HighlightRect(
                    pageNumber = 1,
                    bbox = BoundingBox(
                        x = 10f,
                        y = 20f,
                        width = 100f,
                        height = 12f
                    )
                )
            )
        )
    }

    private class RecordingDocumentPersistenceService(
        private val events: MutableList<String>
    ) : DocumentPersistenceService(
        documentRepository = mock(DocumentRepository::class.java),
        documentChunkRepository = mock(DocumentChunkRepository::class.java)
    ) {
        val processingDocumentIds = mutableListOf<UUID>()
        val readyDocumentIds = mutableListOf<UUID>()
        val failedDocuments = mutableListOf<Pair<UUID, String?>>()
        val savedChunks = mutableListOf<List<DocumentChunkEntity>>()

        override fun markProcessing(documentId: UUID) {
            events += "markProcessing"
            processingDocumentIds += documentId
        }

        override fun saveChunks(chunks: List<DocumentChunkEntity>): List<DocumentChunkEntity> {
            events += "saveChunks"
            savedChunks += chunks
            return chunks
        }

        override fun markReady(documentId: UUID) {
            events += "markReady"
            readyDocumentIds += documentId
        }

        override fun markFailed(documentId: UUID, errorMessage: String?) {
            events += "markFailed"
            failedDocuments += documentId to errorMessage
        }
    }

    private class RecordingPDFParser(
        private val events: MutableList<String>,
        private val parsedPDF: ParsedPDF? = null,
        private val failure: RuntimeException? = null
    ) : PDFParser() {
        var parsedPath: Path? = null
        var parsedDocumentId: UUID? = null

        override fun parse(filepath: Path, documentId: UUID): ParsedPDF {
            events += "parse"
            parsedPath = filepath
            parsedDocumentId = documentId
            failure?.let { throw it }
            return requireNotNull(parsedPDF)
        }
    }

    private class RecordingChunker(
        private val events: MutableList<String>,
        private val chunks: List<PdfChunk>
    ) : Chunker() {
        override fun chunkPDF(parsedPDF: ParsedPDF): List<PdfChunk> {
            events += "chunk"
            return chunks
        }
    }

    private class RecordingEmbeddingService(
        private val events: MutableList<String>,
        private val embeddedChunks: List<EmbeddedChunk> = emptyList(),
        private val failure: RuntimeException? = null
    ) : EmbeddingService(
        embeddingClient = NoopEmbeddingClient(),
        properties = EmbeddingProperties(
            baseUrl = "http://localhost:11434",
            model = "test-embedding"
        )
    ) {
        override fun embedPdfChunks(userId: UUID, chunks: List<PdfChunk>): List<EmbeddedChunk> {
            events += "embed"
            failure?.let { throw it }
            return embeddedChunks
        }
    }

    private class RecordingVectorStorage(
        private val events: MutableList<String>,
        private val failure: RuntimeException? = null
    ) : VectorStorage {
        val savedBatches = mutableListOf<List<EmbeddedChunk>>()

        override fun saveAll(chunks: List<EmbeddedChunk>) {
            events += "saveVectors"
            failure?.let { throw it }
            savedBatches += chunks
        }

        override fun deleteAllByDocumentIdAndUserId(userId: UUID, documentId: UUID) = Unit

        override fun search(
            userId: UUID,
            documentId: UUID?,
            query: String,
            vector: List<Float>,
            limit: Int,
            minimumScore: Float?
        ): List<VectorSearchMatch> = emptyList()
    }

    private class NoopEmbeddingClient : EmbeddingClient {
        override fun embed(texts: List<String>): List<List<Float>> {
            return emptyList()
        }
    }
}
