package org.ai_processor.document.api

import org.ai_processor.document.persistence.exceptions.ChunkNotFoundException
import org.ai_processor.document.persistence.exceptions.DocumentNotFoundException
import org.ai_processor.document.services.DocumentUploadException
import org.ai_processor.document.services.UnsupportedDocumentContentTypeException
import org.ai_processor.processing.embeddings.EmbeddingException
import org.ai_processor.processing.pdfreader.PdfParsingException
import org.ai_processor.storage.FileStorageException
import org.ai_processor.vector_storage.VectorStorageException
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

    @ExceptionHandler(ChunkNotFoundException::class)
    fun handleChunkNotFound(
        exception: ChunkNotFoundException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(exception.message ?: "Chunk not found")
    }

    @ExceptionHandler(UnsupportedDocumentContentTypeException::class)
    fun handleUnsupportedDocumentContentType(
        exception: UnsupportedDocumentContentTypeException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(exception.message ?: "Unsupported document content type")
    }

    @ExceptionHandler(DocumentUploadException::class)
    fun handleDocumentUpload(
        exception: DocumentUploadException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(exception.message ?: "Invalid document upload")
    }

    @ExceptionHandler(PdfParsingException::class)
    fun handlePdfParsing(
        exception: PdfParsingException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(exception.message ?: "Failed to parse PDF")
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorage(
        exception: FileStorageException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(exception.message ?: "File storage operation failed")
    }

    @ExceptionHandler(EmbeddingException::class)
    fun handleEmbedding(
        exception: EmbeddingException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(exception.message ?: "Embedding service failed")
    }

    @ExceptionHandler(VectorStorageException::class)
    fun handleVectorStorage(
        exception: VectorStorageException
    ): ResponseEntity<String> {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(exception.message ?: "Vector storage operation failed")
    }
}
