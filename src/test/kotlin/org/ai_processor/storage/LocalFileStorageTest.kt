package org.ai_processor.storage

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalFileStorageTest {
    @Test
    fun `save writes file under document directory and load reads the same bytes`(
        @TempDir tempDir: Path
    ) {
        val storage = LocalFileStorage(tempDir.toString())
        val documentId = UUID.randomUUID()
        val bytes = "%PDF-1.7 test".toByteArray()

        val storagePath = storage.save(
            documentId = documentId,
            originalFilename = "document.pdf",
            bytes = bytes
        )

        val expectedPath = tempDir
            .resolve(documentId.toString())
            .resolve("document.pdf")
        assertEquals(expectedPath.toString(), storagePath)
        assertTrue(Files.exists(expectedPath))
        assertContentEquals(bytes, storage.load(storagePath))
    }

    @Test
    fun `delete removes an existing stored file`(
        @TempDir tempDir: Path
    ) {
        val storage = LocalFileStorage(tempDir.toString())
        val storagePath = storage.save(
            documentId = UUID.randomUUID(),
            originalFilename = "document.pdf",
            bytes = "content".toByteArray()
        )

        storage.delete(storagePath)

        assertFalse(Files.exists(Path.of(storagePath)))
    }

    @Test
    fun `load missing file throws FileStorageException`(
        @TempDir tempDir: Path
    ) {
        val storage = LocalFileStorage(tempDir.toString())

        assertFailsWith<FileStorageException> {
            storage.load(tempDir.resolve("missing.pdf").toString())
        }
    }

    @Test
    fun `save throws FileStorageException when local root is a file`(
        @TempDir tempDir: Path
    ) {
        val rootFile = tempDir.resolve("not-a-directory")
        Files.write(rootFile, "root".toByteArray())
        val storage = LocalFileStorage(rootFile.toString())

        assertFailsWith<FileStorageException> {
            storage.save(
                documentId = UUID.randomUUID(),
                originalFilename = "document.pdf",
                bytes = "content".toByteArray()
            )
        }
    }

    @Test
    fun `delete wraps filesystem failure in FileStorageException`(
        @TempDir tempDir: Path
    ) {
        val storage = LocalFileStorage(tempDir.toString())
        val nonEmptyDirectory = tempDir.resolve("non-empty")
        Files.createDirectories(nonEmptyDirectory)
        Files.write(nonEmptyDirectory.resolve("child.txt"), "content".toByteArray())

        assertFailsWith<FileStorageException> {
            storage.delete(nonEmptyDirectory.toString())
        }
    }
}
