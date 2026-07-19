package org.ai_processor.vector_storage

open class VectorStorageException (
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class VectorStorageInterruptedException(
    message: String,
    cause: Throwable
) : VectorStorageException(message, cause)

class VectorStorageSearchException(
    message: String,
    cause: Throwable? = null
) : VectorStorageException(message, cause)

class VectorStorageInitializationException(
    message: String,
    cause: Throwable? = null
) : VectorStorageException(message, cause)
