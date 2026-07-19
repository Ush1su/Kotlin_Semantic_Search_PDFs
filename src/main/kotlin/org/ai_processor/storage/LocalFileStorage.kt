package org.ai_processor.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.UUID
import java.nio.file.Files
import java.nio.file.Path


@Service
class LocalFileStorage(
    @Value("\${app.storage.local-root}")
    private val localRoot: String
) : FileStorage {
    override fun save(
        documentId: UUID,
        originalFilename: String,
        bytes: ByteArray
    ) : String {
        try {
            val documentDir = Path.of(localRoot, documentId.toString())
            Files.createDirectories(documentDir)

            val filePath = documentDir.resolve(originalFilename)
            Files.write(filePath, bytes)

            return filePath.toString()
        } catch (exception: IOException) {
            throw FileStorageException(
                message = "Failed to save document file",
                cause = exception
            )
        }
    }

    override fun load(storagePath: String) : ByteArray {
        try {
            return Files.readAllBytes(Path.of(storagePath))
        } catch (exception: IOException) {
            throw FileStorageException(
                message = "Failed to load document file: $storagePath",
                cause = exception
            )
        }
    }

    override fun delete(storagePath: String) {
        try {
            Files.deleteIfExists(Path.of(storagePath))
        } catch (exception: IOException) {
            throw FileStorageException(
                message = "Failed to delete document file: $storagePath",
                cause = exception
            )
        }
    }
}
