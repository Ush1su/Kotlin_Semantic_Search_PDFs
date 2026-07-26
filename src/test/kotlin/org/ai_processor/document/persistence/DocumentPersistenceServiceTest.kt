package org.ai_processor.document.persistence

import org.ai_processor.document.persistence.exceptions.ChunkNotFoundException
import org.ai_processor.document.persistence.exceptions.DocumentNotFoundException
import org.ai_processor.document.persistence.model.DocumentChunkEntity
import org.ai_processor.document.persistence.model.DocumentEntity
import org.ai_processor.document.persistence.model.DocumentStatus
import org.ai_processor.document.persistence.repository.DocumentChunkRepository
import org.ai_processor.document.persistence.repository.DocumentRepository
import org.ai_processor.processing.chunking.HighlightRect
import org.ai_processor.processing.pdfreader.BoundingBox
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentPersistenceServiceTest {
    private val documentRepository = mock(DocumentRepository::class.java)
    private val chunkRepository = mock(DocumentChunkRepository::class.java)
    private val persistenceService = DocumentPersistenceService(
        documentRepository = documentRepository,
        documentChunkRepository = chunkRepository
    )

    @Test
    fun `saveDocument delegates to repository`() {
        val document = documentEntity()
        `when`(documentRepository.save(document)).thenReturn(document)

        val saved = persistenceService.saveDocument(document)

        assertEquals(document, saved)
        verify(documentRepository).save(document)
    }

    @Test
    fun `saveChunks delegates to repository`() {
        val chunks = listOf(documentChunkEntity(chunkIndex = 1))
        `when`(chunkRepository.saveAll(chunks)).thenReturn(chunks)

        val saved = persistenceService.saveChunks(chunks)

        assertEquals(chunks, saved)
        verify(chunkRepository).saveAll(chunks)
    }

    @Test
    fun `getDocumentById returns repository document`() {
        val document = documentEntity()
        `when`(documentRepository.findById(document.id))
            .thenReturn(Optional.of(document))

        val found = persistenceService.getDocumentById(document.id)

        assertEquals(document, found)
    }

    @Test
    fun `markProcessing changes status to processing`() {
        val document = documentEntity(status = DocumentStatus.UPLOADED)
        `when`(documentRepository.findById(document.id))
            .thenReturn(Optional.of(document))

        persistenceService.markProcessing(document.id)

        assertEquals(DocumentStatus.PROCESSING, document.status)
    }

    @Test
    fun `markReady changes status, sets processed time, and clears error`() {
        val document = documentEntity(
            status = DocumentStatus.PROCESSING,
            errorMessage = "previous failure"
        )
        `when`(documentRepository.findById(document.id))
            .thenReturn(Optional.of(document))

        persistenceService.markReady(document.id)

        assertEquals(DocumentStatus.READY, document.status)
        assertNotNull(document.processedAt)
        assertNull(document.errorMessage)
    }

    @Test
    fun `markFailed changes status, sets processed time, and stores error message`() {
        val document = documentEntity(status = DocumentStatus.PROCESSING)
        `when`(documentRepository.findById(document.id))
            .thenReturn(Optional.of(document))

        persistenceService.markFailed(document.id, "embedding failed")

        assertEquals(DocumentStatus.FAILED, document.status)
        assertNotNull(document.processedAt)
        assertEquals("embedding failed", document.errorMessage)
    }

    @Test
    fun `status updates throw DocumentNotFoundException when document is missing`() {
        val documentId = UUID.randomUUID()
        `when`(documentRepository.findById(documentId))
            .thenReturn(Optional.empty())

        assertFailsWith<DocumentNotFoundException> {
            persistenceService.markProcessing(documentId)
        }
        assertFailsWith<DocumentNotFoundException> {
            persistenceService.markReady(documentId)
        }
        assertFailsWith<DocumentNotFoundException> {
            persistenceService.markFailed(documentId, "failed")
        }
    }

    @Test
    fun `deleteDocumentData deletes chunks before document row`() {
        val document = documentEntity()
        `when`(documentRepository.findByIdAndUserId(document.id, document.userId))
            .thenReturn(document)

        persistenceService.deleteDocumentData(document.id, document.userId)

        val order = inOrder(chunkRepository, documentRepository)
        order.verify(chunkRepository).deleteByDocumentId(document.id)
        order.verify(documentRepository).deleteById(document.id)
    }

    @Test
    fun `deleteDocumentData throws DocumentNotFoundException when document is missing`() {
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        `when`(documentRepository.findByIdAndUserId(documentId, userId))
            .thenReturn(null)

        assertFailsWith<DocumentNotFoundException> {
            persistenceService.deleteDocumentData(documentId, userId)
        }
    }

    @Test
    fun `getStoragePath returns path for existing document`() {
        val document = documentEntity(storagePath = "/documents/file.pdf")
        `when`(documentRepository.findByIdAndUserId(document.id, document.userId))
            .thenReturn(document)

        val storagePath = persistenceService.getStoragePath(document.id, document.userId)

        assertEquals("/documents/file.pdf", storagePath)
    }

    @Test
    fun `getStoragePath throws DocumentNotFoundException when document is missing`() {
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        `when`(documentRepository.findByIdAndUserId(documentId, userId))
            .thenReturn(null)

        assertFailsWith<DocumentNotFoundException> {
            persistenceService.getStoragePath(documentId, userId)
        }
    }

    @Test
    fun `getChunksByDocumentIdAndUserId returns chunks ordered by repository query`() {
        val document = documentEntity()
        val chunks = listOf(
            documentChunkEntity(documentId = document.id, chunkIndex = 1),
            documentChunkEntity(documentId = document.id, chunkIndex = 2)
        )
        `when`(documentRepository.findByIdAndUserId(document.id, document.userId))
            .thenReturn(document)
        `when`(chunkRepository.findByDocumentIdOrderByChunkIndex(document.id))
            .thenReturn(chunks)

        val found = persistenceService.getChunksByDocumentIdAndUserId(document.id, document.userId)

        assertEquals(chunks, found)
    }

    @Test
    fun `getChunksByDocumentIdAndUserId throws DocumentNotFoundException when document is missing`() {
        val documentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        `when`(documentRepository.findByIdAndUserId(documentId, userId))
            .thenReturn(null)

        assertFailsWith<DocumentNotFoundException> {
            persistenceService.getChunksByDocumentIdAndUserId(documentId, userId)
        }
    }

    @Test
    fun `getChunkByIdAndUserId returns chunk when its document belongs to the user`() {
        val document = documentEntity()
        val chunk = documentChunkEntity(documentId = document.id, chunkIndex = 1)
        `when`(chunkRepository.findById(chunk.id))
            .thenReturn(Optional.of(chunk))
        `when`(documentRepository.findByIdAndUserId(document.id, document.userId))
            .thenReturn(document)

        val found = persistenceService.getChunkByIdAndUserId(chunk.id, document.userId)

        assertEquals(chunk, found)
    }


    @Test
    fun `getChunkByIdAndUserId throws ChunkNotFoundException when chunk's document does not belong to the user`() {
        val document = documentEntity()
        val chunk = documentChunkEntity(documentId = document.id, chunkIndex = 1)
        val otherUserId = UUID.randomUUID()
        `when`(chunkRepository.findById(chunk.id))
            .thenReturn(Optional.of(chunk))
        `when`(documentRepository.findByIdAndUserId(document.id, otherUserId))
            .thenReturn(null)

        assertFailsWith<ChunkNotFoundException> {
            persistenceService.getChunkByIdAndUserId(chunk.id, otherUserId)
        }
    }

    private fun documentEntity(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        status: DocumentStatus = DocumentStatus.UPLOADED,
        storagePath: String = "/documents/$id/original.pdf",
        errorMessage: String? = null
    ): DocumentEntity {
        return DocumentEntity(
            id = id,
            userId = userId,
            originalFilename = "original.pdf",
            contentType = "application/pdf",
            fileSizeBytes = 100,
            storagePath = storagePath,
            status = status,
            errorMessage = errorMessage,
            createdAt = Instant.parse("2026-01-01T00:00:00Z")
        )
    }

    private fun documentChunkEntity(
        id: UUID = UUID.randomUUID(),
        documentId: UUID = UUID.randomUUID(),
        chunkIndex: Int
    ): DocumentChunkEntity {
        return DocumentChunkEntity(
            id = id,
            documentId = documentId,
            chunkIndex = chunkIndex,
            text = "chunk $chunkIndex",
            pageStart = 1,
            pageEnd = 1,
            highlightRects = listOf(
                HighlightRect(
                    pageNumber = 1,
                    bbox = BoundingBox(10f, 20f, 100f, 12f)
                )
            )
        )
    }
}
