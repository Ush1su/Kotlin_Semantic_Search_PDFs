package org.ai_processor.vector_storage

import org.ai_processor.processing.embeddings.EmbeddedChunk
import java.util.UUID

interface VectorStorage {
    fun saveAll(chunks: List<EmbeddedChunk>)
    fun deleteAllByDocumentId(documentId: UUID)
}