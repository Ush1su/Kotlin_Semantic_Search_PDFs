package org.ai_processor.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID
import java.nio.file.Files
import java.nio.file.Path


@Service
class LocalFileStorage(
    @Value("\${spring.app.storage.local-root}")
    private val localRoot: String
) : FileStorage {
    override fun save(
        documentId: UUID,
        originalFilename: String,
        bytes: ByteArray
    ) : String {
        val documentDir = Path.of(localRoot, documentId.toString())
        Files.createDirectories(documentDir)

        val filePath = documentDir.resolve(originalFilename)
        Files.write(filePath, bytes)

        return filePath.toString()
    }

    override fun load(storagePath: String) : ByteArray {
        return Files.readAllBytes(Path.of(storagePath))
    }

    override fun delete(storagePath: String) {
        Files.delete(Path.of(storagePath))
    }
}