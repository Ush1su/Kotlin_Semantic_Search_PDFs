package org.ai_processor.document.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.EnumType
import java.time.Instant
import java.util.UUID

enum class DocumentStatus {
    UPLOADED,
    PROCESSING,
    READY,
    FAILED
}

@Entity
@Table(name = "documents")
class DocumentEntity(
    @Id
    val id: UUID,

    @Column(name = "original_filename", nullable = false)
    val originalFilename: String,

    @Column(name="content_type", nullable = false)
    val contentType: String,

    @Column(name="file_size_bytes", nullable = false)
    val fileSizeBytes: Long,

    @Column(name="storage_path", nullable = false)
    val storagePath: String,

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    var status: DocumentStatus,

    @Column(name="error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name="created_at", nullable = false)
    val createdAt: Instant,

    @Column(name="processed_at")
    var processedAt: Instant? = null
)