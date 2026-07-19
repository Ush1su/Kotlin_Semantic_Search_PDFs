package org.ai_processor.document.services

open class DocumentUploadException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class EmptyDocumentUploadException :
    DocumentUploadException("File is empty")

class UnsupportedDocumentContentTypeException(
    contentType: String?
) : DocumentUploadException(
    "Only PDF files are supported. Received content type: ${contentType ?: "unknown"}"
)
