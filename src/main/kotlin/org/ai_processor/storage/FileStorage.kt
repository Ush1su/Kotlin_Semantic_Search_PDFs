package org.ai_processor.storage

import java.util.UUID

interface FileStorage {
    fun save(documentId: UUID, originalFilename: String, bytes: ByteArray): String
    fun load(storagePath: String): ByteArray
    fun delete(storagePath: String)
}