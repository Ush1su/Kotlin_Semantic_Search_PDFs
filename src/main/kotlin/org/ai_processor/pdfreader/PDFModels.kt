package org.ai_processor.pdfreader

import java.util.UUID

data class ParsedPdf(
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
    val text: String,
    val pageNumber: Int,
    val bbox: BoundingBox,
    val words: List<WordToken>
)

data class WordToken(
    val text: String,
    val pageNumber: Int,
    val bbox: BoundingBox
)

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)