package org.ai_processor.services

import org.ai_processor.chunking.Chunker
import org.ai_processor.persistence.DocumentPersistenceService
import org.ai_processor.persistence.model.DocumentChunkEntity
import org.ai_processor.storage.FileStorage
import org.ai_processor.pdfreader.PDFParser
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.UUID

@Service
class ProcessDocumentService(
    private val documentPersistenceService: DocumentPersistenceService,
    private val fileStorage: FileStorage,
    private val pdfParser: PDFParser,
    private val chunker: Chunker
) {

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
            documentPersistenceService.markReady(documentId)
        } catch (e: Exception) {
            documentPersistenceService.markFailed(documentId, e.message)
            throw e
        }
    }
}