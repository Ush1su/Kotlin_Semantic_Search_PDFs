package org.ai_processor.persistence

import java.util.UUID

class DocumentNotFoundException(
    documentId: UUID
) : RuntimeException("Document with id $documentId was not found")