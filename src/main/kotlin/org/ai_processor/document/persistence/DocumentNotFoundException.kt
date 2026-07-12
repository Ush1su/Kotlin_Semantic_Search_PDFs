package org.ai_processor.document.persistence

import java.util.UUID

class DocumentNotFoundException(
    documentId: UUID
) : RuntimeException("Document with id $documentId was not found")