package org.ai_processor.document.services

import org.ai_processor.document.persistence.DocumentPersistenceService
import org.ai_processor.document.persistence.model.DocumentEntity
import org.ai_processor.document.persistence.model.DocumentStatus
import org.ai_processor.storage.FileStorage
import org.ai_processor.vector_storage.QdrantService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory


@Service
class DocumentService(
    private val fileStorage: FileStorage,
    private val documentPersistenceService: DocumentPersistenceService,
    private val qdrantService: QdrantService,
    private val processDocumentService: ProcessDocumentService
) {
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)

    fun upload(file: MultipartFile): UUID {
        if (file.isEmpty) {
            throw EmptyDocumentUploadException()
        }
        if (file.contentType != "application/pdf") {
            throw UnsupportedDocumentContentTypeException(file.contentType)
        }

        val documentId = UUID.randomUUID()
        val originalFilename = file.originalFilename ?: "document.pdf"
        val bytes = file.bytes
        val now = Instant.now()

        val storagePath = fileStorage.save(
            documentId = documentId,
            originalFilename = originalFilename,
            bytes = bytes
        )

        val documentEntity = DocumentEntity(
            id = documentId,
            originalFilename = originalFilename,
            contentType = file.contentType ?: "application/pdf",
            fileSizeBytes = bytes.size.toLong(),
            storagePath = storagePath,
            createdAt = now,
            status = DocumentStatus.UPLOADED
        )
        documentPersistenceService.saveDocument(documentEntity)
        logger.info("Document uploaded: $documentId")
        processDocumentService.process(documentId, storagePath)
        logger.info("Document processing started: $documentId")
        return documentId
    }

    fun delete(documentId: UUID) {
        val storagePath = documentPersistenceService.getStoragePath(documentId)

        fileStorage.delete(storagePath)
        documentPersistenceService.deleteDocumentData(documentId)
        qdrantService.deleteAllByDocumentId(documentId)
    }
}
