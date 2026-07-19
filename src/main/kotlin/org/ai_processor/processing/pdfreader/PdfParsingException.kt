package org.ai_processor.processing.pdfreader

class PdfParsingException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
