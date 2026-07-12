package org.ai_processor.document.api

import org.ai_processor.document.persistence.DocumentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException::class)
    fun handleDocumentNotFound(
        exception: DocumentNotFoundException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(exception.message ?: "Document not found")
    }
}