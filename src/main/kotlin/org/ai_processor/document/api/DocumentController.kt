package org.ai_processor.document.api

import org.ai_processor.document.api.model.DocumentChunkResponse
import org.ai_processor.document.api.model.DocumentResponse
import org.ai_processor.document.api.model.DocumentUploadResponse
import org.ai_processor.document.persistence.DocumentNotFoundException
import org.ai_processor.document.services.DocumentService
import org.ai_processor.document.persistence.DocumentPersistenceService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val documentService: DocumentService,
    private val documentPersistenceService: DocumentPersistenceService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadDocument(
        @RequestParam("file") file: MultipartFile
    ): DocumentUploadResponse {
        val documentId = documentService.upload(file)

        return DocumentUploadResponse(
            documentId = documentId,
            status = "UPLOADED"
        )
    }

    @GetMapping("/{documentId}")
    fun getDocument(
        @PathVariable documentId: UUID
    ): DocumentResponse {
        val document = documentPersistenceService.getDocumentById(documentId)
            ?: throw DocumentNotFoundException(documentId)

        return DocumentResponse(
            id = document.id,
            originalFilename = document.originalFilename,
            contentType = document.contentType,
            fileSizeBytes = document.fileSizeBytes,
            storagePath = document.storagePath,
            status = document.status,
            errorMessage = document.errorMessage,
            createdAt = document.createdAt.toString(),
            processedAt = document.processedAt?.toString()
        )
    }

    @GetMapping("/{documentId}/chunks")
    fun getDocumentChunks(
        @PathVariable documentId: UUID
    ): List<DocumentChunkResponse> {
        return documentPersistenceService.getChunksByDocumentId(documentId)
            .map { chunk ->
                DocumentChunkResponse(
                    id = chunk.id,
                    documentId = chunk.documentId,
                    chunkIndex = chunk.chunkIndex,
                    text = chunk.text,
                    pageStart = chunk.pageStart,
                    pageEnd = chunk.pageEnd
                )
            }
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteDocument(
        @PathVariable documentId: UUID
    ) {
        documentService.delete(documentId)
    }
}
