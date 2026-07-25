package org.ai_processor.document.services

import org.ai_processor.processing.embeddings.EmbeddingService
import org.ai_processor.vector_storage.model.VectorSearchMatch
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DocumentSearchServiceTest {
    private val embeddingService = mock(EmbeddingService::class.java)
    private val vectorStorage = RecordingVectorStorage()
    private val searchService = DocumentSearchService(
        embeddingService = embeddingService,
        vectorStorage = vectorStorage
    )

    @Test
    fun `search embeds query and delegates vector search`() {
        val userId = UUID.randomUUID()
        val query = "linear algebra eigenvectors"
        val embedding = listOf(0.1f, 0.2f, 0.3f)
        val expectedMatch = VectorSearchMatch(
            chunkId = UUID.randomUUID(),
            documentId = UUID.randomUUID(),
            text = "Eigenvectors are non-zero vectors...",
            score = 0.91f
        )
        vectorStorage.searchResults = listOf(expectedMatch)
        `when`(embeddingService.embedText(query)).thenReturn(embedding)

        val results = searchService.search(
            userId = userId,
            query = query,
            limit = 3,
            minimumScore = 0.7f
        )

        assertEquals(listOf(expectedMatch), results)
        assertEquals(
            listOf(RecordingVectorStorage.Search(userId, null, embedding, 3, 0.7f, query)),
            vectorStorage.searches
        )
    }
}
