package org.ai_processor.storage

class FileStorageException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
