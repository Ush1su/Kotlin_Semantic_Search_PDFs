package org.ai_processor.processing.embeddings

interface EmbeddingClient {
    fun embed(texts: List<String>): List<List<Float>>
}