package org.ai_processor.document.services

import org.ai_processor.document.persistence.DocumentPersistenceService
import org.ai_processor.document.persistence.model.DocumentEntity
import org.ai_processor.document.persistence.model.DocumentStatus
import org.ai_processor.document.persistence.repository.DocumentChunkRepository
import org.ai_processor.document.persistence.repository.DocumentRepository
import org.ai_processor.storage.FileStorage
import org.ai_processor.vector_storage.VectorStorage
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.mockito.Mockito.mock
import org.springframework.mock.web.MockMultipartFile
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentServiceTest {
    private val fileStorage = RecordingFileStorage()
    private val documentPersistenceService = RecordingDocumentPersistenceService()
    private val vectorStorage = RecordingVectorStorage()
    private val processDocumentService = RecordingProcessDocumentService()
    private val documentService = DocumentService(
        fileStorage = fileStorage,
        documentPersistenceService = documentPersistenceService,
        vectorStorage = vectorStorage,
        processDocumentService = processDocumentService
    )

    @Test
    fun `upload rejects empty files`() {
        val file = MockMultipartFile(
            "file",
            "empty.pdf",
            "application/pdf",
            ByteArray(0)
        )

        assertFailsWith<EmptyDocumentUploadException> {
            documentService.upload(file)
        }

        assertEquals(0, fileStorage.savedFiles.size)
        assertEquals(0, documentPersistenceService.savedDocuments.size)
        assertEquals(0, processDocumentService.processCalls.size)
    }

    @Test
    fun `upload rejects non-PDF files`() {
        val file = MockMultipartFile(
            "file",
            "notes.txt",
            "text/plain",
            "not a pdf".toByteArray()
        )

        assertFailsWith<UnsupportedDocumentContentTypeException> {
            documentService.upload(file)
        }

        assertEquals(0, fileStorage.savedFiles.size)
        assertEquals(0, documentPersistenceService.savedDocuments.size)
        assertEquals(0, processDocumentService.processCalls.size)
    }

    @Test
    fun `upload stores file, saves metadata, and starts processing`() {
        val bytes = "%PDF-1.7".toByteArray()
        val file = MockMultipartFile(
            "file",
            "sample.pdf",
            "application/pdf",
            bytes
        )

        val documentId = documentService.upload(file)

        assertEquals(1, fileStorage.savedFiles.size)
        val savedFile = fileStorage.savedFiles.single()
        assertEquals(documentId, savedFile.documentId)
        assertEquals("sample.pdf", savedFile.originalFilename)
        assertArrayEquals(bytes, savedFile.bytes)

        val savedDocument = documentPersistenceService.savedDocuments.single()
        assertEquals(documentId, savedDocument.id)
        assertEquals("sample.pdf", savedDocument.originalFilename)
        assertEquals("application/pdf", savedDocument.contentType)
        assertEquals(bytes.size.toLong(), savedDocument.fileSizeBytes)
        assertEquals(savedFile.storagePath, savedDocument.storagePath)
        assertEquals(DocumentStatus.UPLOADED, savedDocument.status)

        assertEquals(
            listOf(RecordingProcessDocumentService.ProcessCall(documentId, savedFile.storagePath)),
            processDocumentService.processCalls
        )
    }

    @Test
    fun `upload falls back to default filename when original filename is missing`() {
        val file = MockMultipartFile(
            "file",
            null,
            "application/pdf",
            "%PDF-1.7".toByteArray()
        )

        val documentId = documentService.upload(file)

        assertEquals(documentId, fileStorage.savedFiles.single().documentId)
        assertEquals("document.pdf", fileStorage.savedFiles.single().originalFilename)
    }

    @Test
    fun `delete removes local file, persistence data, and vectors`() {
        val documentId = UUID.randomUUID()
        val storagePath = "/tmp/document.pdf"
        documentPersistenceService.storagePaths[documentId] = storagePath

        documentService.delete(documentId)

        assertEquals(listOf(storagePath), fileStorage.deletedPaths)
        assertEquals(listOf(documentId), documentPersistenceService.deletedDocumentIds)
        assertEquals(listOf(documentId), vectorStorage.deletedDocumentIds)
    }

    private data class SavedFile(
        val documentId: UUID,
        val originalFilename: String,
        val bytes: ByteArray,
        val storagePath: String
    )

    private class RecordingFileStorage : FileStorage {
        val savedFiles = mutableListOf<SavedFile>()
        val deletedPaths = mutableListOf<String>()

        override fun save(
            documentId: UUID,
            originalFilename: String,
            bytes: ByteArray
        ): String {
            val storagePath = "/documents/$documentId/$originalFilename"
            savedFiles += SavedFile(
                documentId = documentId,
                originalFilename = originalFilename,
                bytes = bytes,
                storagePath = storagePath
            )
            return storagePath
        }

        override fun load(storagePath: String): ByteArray {
            return savedFiles.single { it.storagePath == storagePath }.bytes
        }

        override fun delete(storagePath: String) {
            deletedPaths += storagePath
        }
    }

    private class RecordingDocumentPersistenceService : DocumentPersistenceService(
        documentRepository = mock(DocumentRepository::class.java),
        documentChunkRepository = mock(DocumentChunkRepository::class.java)
    ) {
        val savedDocuments = mutableListOf<DocumentEntity>()
        val deletedDocumentIds = mutableListOf<UUID>()
        val storagePaths = mutableMapOf<UUID, String>()

        override fun saveDocument(documentEntity: DocumentEntity): DocumentEntity {
            savedDocuments += documentEntity
            storagePaths[documentEntity.id] = documentEntity.storagePath
            return documentEntity
        }

        override fun getStoragePath(documentId: UUID): String {
            return storagePaths.getValue(documentId)
        }

        override fun deleteDocumentData(documentId: UUID) {
            deletedDocumentIds += documentId
        }
    }

    private class RecordingProcessDocumentService : ProcessDocumentService(
        documentPersistenceService = mock(DocumentPersistenceService::class.java),
        embeddingService = mock(org.ai_processor.processing.embeddings.EmbeddingService::class.java),
        vectorStorage = mock(VectorStorage::class.java),
        pdfParser = mock(org.ai_processor.processing.pdfreader.PDFParser::class.java),
        chunker = mock(org.ai_processor.processing.chunking.Chunker::class.java)
    ) {
        val processCalls = mutableListOf<ProcessCall>()

        override fun process(documentId: UUID, storagePathString: String) {
            processCalls += ProcessCall(documentId, storagePathString)
        }

        data class ProcessCall(
            val documentId: UUID,
            val storagePath: String
        )
    }
}
