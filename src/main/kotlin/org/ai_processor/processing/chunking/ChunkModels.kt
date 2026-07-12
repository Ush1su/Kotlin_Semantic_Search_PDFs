package org.ai_processor.processing.chunking

import org.ai_processor.processing.pdfreader.BoundingBox
import java.util.UUID

data class PdfChunk(
    val id: UUID,
    val documentId: UUID,

    val chunkIndex: Int,
    val text: String,

    val pageStart: Int,
    val pageEnd: Int,

    val highlightRects: List<HighlightRect>
)

data class HighlightRect(
    val pageNumber: Int,
    val bbox: BoundingBox
)

