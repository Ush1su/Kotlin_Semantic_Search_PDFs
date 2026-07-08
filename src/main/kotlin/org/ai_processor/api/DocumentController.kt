package org.ai_processor.api

import org.ai_processor.services.UploadDocumentService
import org.ai_processor.persistence.DocumentPersistenceService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/documents")
class DocumentController(
    private val uploadDocumentService: UploadDocumentService,
    private val documentPersistenceService: DocumentPersistenceService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadDocument(
        @RequestParam("file") file: MultipartFile
    ): DocumentUploadResponse {
        val documentId = try {
            uploadDocumentService.upload(file)
        } catch (_: Exception) {
            return DocumentUploadResponse(
                documentId = null,
                status = "FAILED"
            )
        }

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
            ?: throw IllegalArgumentException("Document not found: $documentId")

        return DocumentResponse(
            id = document.id,
            originalFilename = document.originalFilename,
            contentType = document.contentType,
            fileSizeBytes = document.fileSizeBytes,
            storagePath = document.storagePath,
            status = document.status.name,
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
}