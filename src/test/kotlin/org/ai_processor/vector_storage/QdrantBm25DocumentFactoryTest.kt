package org.ai_processor.vector_storage

import org.ai_processor.vector_storage.config.QdrantProperties
import kotlin.test.Test
import kotlin.test.assertEquals

class QdrantBm25DocumentFactoryTest {
    @Test
    fun `document configures Qdrant BM25 model and tokenizer minimum token length`() {
        val factory = QdrantBm25DocumentFactory(
            QdrantProperties(
                vectorSize = 3,
                bm25TokenMinLength = 4
            )
        )

        val document = factory.document("AI PDF search")

        assertEquals("AI PDF search", document.text)
        assertEquals("qdrant/bm25", document.model)
        assertEquals(4, document.optionsMap["min_token_len"]?.integerValue)
    }
}
