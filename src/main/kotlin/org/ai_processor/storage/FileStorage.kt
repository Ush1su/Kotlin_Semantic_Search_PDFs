package org.ai_processor.storage

import org.springframework.core.io.Resource
import java.util.UUID

interface FileStorage {
    fun save(documentId: UUID, originalFilename: String, bytes: ByteArray): String
    fun loadAsResource(storagePath: String): Resource
    fun delete(storagePath: String)
}