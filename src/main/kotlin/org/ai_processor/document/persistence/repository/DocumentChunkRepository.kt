package org.ai_processor.document.persistence.repository

import org.ai_processor.document.persistence.model.DocumentChunkEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DocumentChunkRepository : JpaRepository<DocumentChunkEntity, UUID> {
    fun findByDocumentIdAndChunkIndex(documentId: UUID, chunkIndex: Int): DocumentChunkEntity?
    fun findByDocumentId(documentId: UUID): List<DocumentChunkEntity>
    fun deleteByDocumentId(documentId: UUID)
    fun findByIdIn(documentIds: Collection<UUID>): List<DocumentChunkEntity>
    fun findByDocumentIdOrderByChunkIndex(documentId: UUID): List<DocumentChunkEntity>
}