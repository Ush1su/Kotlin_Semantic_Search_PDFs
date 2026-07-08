package org.ai_processor.api

import java.util.UUID

data class DocumentUploadResponse(
    val documentId: UUID?,
    val status: String
)

data class DocumentResponse(
    val id: UUID,
    val originalFilename: String,
    val contentType: String,
    val fileSizeBytes: Long,
    val storagePath: String,
    val status: String,
    val errorMessage: String?,
    val createdAt: String,
    val processedAt: String?
)

data class DocumentChunkResponse(
    val id: UUID,
    val documentId: UUID,
    val chunkIndex: Int,
    val text: String,
    val pageStart: Int,
    val pageEnd: Int
)