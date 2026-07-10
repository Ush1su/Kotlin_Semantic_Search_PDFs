package org.ai_processor.services

import org.ai_processor.persistence.DocumentPersistenceService
import org.ai_processor.persistence.model.DocumentEntity
import org.ai_processor.persistence.model.DocumentStatus
import org.ai_processor.storage.FileStorage
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory


@Service
class UploadDocumentService(
    private val fileStorage: FileStorage,
    private val documentPersistenceService: DocumentPersistenceService,
    private val processDocumentService: ProcessDocumentService
) {
    private val logger = LoggerFactory.getLogger(UploadDocumentService::class.java)

    fun upload(file: MultipartFile): UUID {
        require(!file.isEmpty) { "File is empty" }
        require(file.contentType == "application/pdf") { "Only PDF files are supported" }

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

        return documentId
    }
}