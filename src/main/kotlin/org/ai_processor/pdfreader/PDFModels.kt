package org.ai_processor.pdfreader

import java.util.UUID

data class ParsedPDF(
    val documentId: UUID,
    val pages: List<ParsedPage>
)

data class ParsedPage(
    val pageNumber: Int,
    val width: Float,
    val height: Float,
    val textBlocks: List<TextBlock>
)

data class TextBlock(
    val blockIndex: Int,
    val text: String,
    val pageNumber: Int,
    val bbox: BoundingBox,
)

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class RawTextPiece(
    val value: String,
    val pageNumber: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)