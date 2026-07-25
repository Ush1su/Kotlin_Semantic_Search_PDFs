package org.ai_processor.document.api

import org.ai_processor.auth.CurrentUserProvider
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
    private val documentPersistenceService: DocumentPersistenceService,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadDocument(
        @RequestParam("file") file: MultipartFile
    ): DocumentUploadResponse {
        val userId = currentUserProvider.currentUserId()
        val documentId = documentService.upload(file, userId)

        return DocumentUploadResponse(
            documentId = documentId,
            status = "UPLOADED"
        )
    }

    @GetMapping("/{documentId}")
    fun getDocument(
        @PathVariable documentId: UUID
    ): DocumentResponse {
        val userId = currentUserProvider.currentUserId()
        val document = documentPersistenceService.getDocumentByIdAndUserId(documentId, userId)
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
        val userId = currentUserProvider.currentUserId()
        return documentPersistenceService.getChunksByDocumentIdAndUserId(documentId, userId)
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
        val userId = currentUserProvider.currentUserId()
        documentService.delete(documentId, userId)
    }
}
