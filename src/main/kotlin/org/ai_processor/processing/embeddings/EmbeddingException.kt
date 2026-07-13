package org.ai_processor.processing.embeddings

class EmbeddingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)