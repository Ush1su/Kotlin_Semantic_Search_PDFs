package org.ai_processor.document.persistence.exceptions

import java.util.UUID

class ChunkNotFoundException(
    chunkId: UUID
) : RuntimeException("Chunk not found with id: $chunkId")