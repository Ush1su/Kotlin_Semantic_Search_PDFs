package org.ai_processor.vector_storage

import io.qdrant.client.ValueFactory.value
import io.qdrant.client.grpc.Points.Document
import org.ai_processor.vector_storage.config.QdrantProperties
import org.springframework.stereotype.Component

@Component
class QdrantBm25DocumentFactory(
    private val properties: QdrantProperties
) {
    fun document(text: String): Document {
        return Document.newBuilder()
            .setText(text)
            .setModel(BM25_MODEL)
            .putOptions(MIN_TOKEN_LEN_OPTION, value(properties.bm25TokenMinLength.toLong()))
            .build()
    }

    companion object {
        const val BM25_MODEL = "qdrant/bm25"
        const val MIN_TOKEN_LEN_OPTION = "min_token_len"
    }
}
