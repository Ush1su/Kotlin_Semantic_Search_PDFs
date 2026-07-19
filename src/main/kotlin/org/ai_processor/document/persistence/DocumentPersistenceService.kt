package org.ai_processor.document.persistence

import org.ai_processor.document.persistence.model.DocumentChunkEntity
import org.ai_processor.document.persistence.model.DocumentEntity
import org.ai_processor.document.persistence.model.DocumentStatus
import org.ai_processor.document.persistence.repository.DocumentChunkRepository
import org.ai_processor.document.persistence.repository.DocumentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class DocumentPersistenceService(
    private val documentRepository: DocumentRepository,
    private val documentChunkRepository: DocumentChunkRepository
) {
    @Transactional
    fun saveDocument(documentEntity: DocumentEntity) : DocumentEntity {
        return documentRepository.save(documentEntity)
    }

    @Transactional
    fun saveChunks(chunks: List<DocumentChunkEntity>) : List<DocumentChunkEntity> {
        return documentChunkRepository.saveAll(chunks)
    }

    @Transactional(readOnly = true)
    fun getDocumentById(id: UUID) : DocumentEntity? {
        return documentRepository.findByIdOrNull(id)
    }

    @Transactional
    fun markProcessing(documentId: UUID) {
        val document = getDocumentById(documentId) ?: throw DocumentNotFoundException(documentId)
        document.status = DocumentStatus.PROCESSING
    }

    @Transactional
    fun markReady(documentId: UUID) {
        val now = Instant.now()

        val document = getDocumentById(documentId) ?: throw DocumentNotFoundException(documentId)
        document.status = DocumentStatus.READY
        document.processedAt = now
        document.errorMessage = null
    }

    @Transactional
    fun markFailed(documentId: UUID, errorMessage: String?) {
        val document = getDocumentById(documentId) ?: throw DocumentNotFoundException(documentId)
        document.status = DocumentStatus.FAILED
        document.processedAt = Instant.now()
        document.errorMessage = errorMessage
    }

    @Transactional
    fun deleteDocumentData(documentId: UUID) {
        documentChunkRepository.deleteByDocumentId(documentId)
        documentRepository.deleteById(documentId)
    }

    @Transactional(readOnly = true)
    fun getStoragePath(documentId: UUID): String {
        val document = documentRepository.findByIdOrNull(documentId) ?: throw DocumentNotFoundException(documentId)
        return document.storagePath
    }

    @Transactional(readOnly = true)
    fun getChunksByDocumentId(documentId: UUID): List<DocumentChunkEntity> {
        return documentChunkRepository.findByDocumentIdOrderByChunkIndex(documentId)
    }
}
