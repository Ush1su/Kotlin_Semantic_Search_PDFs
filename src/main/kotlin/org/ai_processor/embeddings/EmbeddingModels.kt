package org.ai_processor.embeddings

import org.ai_processor.pdfreader.BoundingBox
import java.util.UUID

data class PdfChunk(
    val id: UUID,
    val documentId: UUID,

    val chunkIndex: Int,
    val text: String,

    val pageStart: Int,
    val pageEnd: Int,

    val wordRefs: List<WordTokenRef>,
    val highlightRects: List<HighlightRect>
)

data class WordTokenRef(
    val pageNumber: Int,
    val blockIndex: Int,
    val wordIndex: Int
)

data class HighlightRect(
    val pageNumber: Int,
    val bbox: BoundingBox
)

