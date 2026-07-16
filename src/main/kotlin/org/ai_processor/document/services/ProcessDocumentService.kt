package org.ai_processor.document.services

import org.ai_processor.processing.chunking.Chunker
import org.ai_processor.document.persistence.DocumentPersistenceService
import org.ai_processor.document.persistence.model.DocumentChunkEntity
import org.ai_processor.processing.embeddings.EmbeddingService
import org.ai_processor.processing.pdfreader.PDFParser
import org.ai_processor.vector_storage.QdrantService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.UUID

@Service
class ProcessDocumentService(
    private val documentPersistenceService: DocumentPersistenceService,
    private val embeddingService: EmbeddingService,
    private val qdrantService: QdrantService,
    private val pdfParser: PDFParser,
    private val chunker: Chunker
) {

    private val logger = LoggerFactory.getLogger(ProcessDocumentService::class.java)

    @Async("documentProcessingExecutor")
    fun process(documentId: UUID, storagePathString: String) {
        try {
            documentPersistenceService.markProcessing(documentId)
            val storagePath = Path.of(storagePathString)
            val parsedPdf = pdfParser.parse(storagePath)

            val chunks = chunker.chunkPDF(parsedPdf)
            val chunkEntities = chunks.map { chunk ->
                DocumentChunkEntity(
                    id = chunk.id,
                    documentId = chunk.documentId,
                    chunkIndex = chunk.chunkIndex,
                    text = chunk.text,
                    pageStart = chunk.pageStart,
                    pageEnd = chunk.pageEnd,
                    highlightRects = chunk.highlightRects,
                )
            }
            documentPersistenceService.saveChunks(chunkEntities)
            val embeddedChunks = embeddingService.embedPdfChunks(chunks)
            qdrantService.saveAll(embeddedChunks)
            documentPersistenceService.markReady(documentId)
        } catch (e: Exception) {
            logger.error("Error processing document: $documentId", e)
            documentPersistenceService.markFailed(documentId, e.message)
            throw e
        }
    }
}