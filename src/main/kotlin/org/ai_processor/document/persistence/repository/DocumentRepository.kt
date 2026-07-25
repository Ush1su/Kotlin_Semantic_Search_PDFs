package org.ai_processor.document.persistence.repository

import org.ai_processor.document.persistence.model.DocumentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DocumentRepository : JpaRepository<DocumentEntity, UUID> {
    fun findByOriginalFilenameContainingIgnoreCase(
        originalFilename: String
    ): List<DocumentEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: UUID
    ): DocumentEntity?

    fun deleteByIdAndUserId(
        id: UUID,
        userId: UUID
    ): Unit
}