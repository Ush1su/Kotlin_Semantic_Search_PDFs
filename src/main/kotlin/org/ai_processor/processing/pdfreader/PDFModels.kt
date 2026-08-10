package org.ai_processor.processing.pdfreader

import java.util.UUID

data class ParsedPDF(
    val documentId: UUID,
    val numberOfPages: Int,
    val title: String?,
    val author: String?,
    val blocks: List<ParsedBlock>
)

sealed interface ParsedBlock {
    val blockIndex: Int
    val sourceId: Long?
    val pageNumber: Int
    val bbox: BoundingBox
}

data class ParagraphBlock(
    override val blockIndex: Int,
    override val sourceId: Long?,
    override val pageNumber: Int,
    override val bbox: BoundingBox,

    val text: String
) : ParsedBlock

data class HeadingBlock(
    override val blockIndex: Int,
    override val sourceId: Long?,
    override val pageNumber: Int,
    override val bbox: BoundingBox,

    val text: String,
    val headingLevel: Int
) : ParsedBlock

data class CaptionBlock(
    override val blockIndex: Int,
    override val sourceId: Long?,
    override val pageNumber: Int,
    override val bbox: BoundingBox,

    val text: String,
    val linkedContentId: Long?
) : ParsedBlock

data class ListBlock(
    override val blockIndex: Int,
    override val sourceId: Long?,
    override val pageNumber: Int,
    override val bbox: BoundingBox,

    val items: List<ParsedListItem>
) : ParsedBlock

data class ParsedListItem(
    val text: String,
    val pageNumber: Int,
    val bbox: BoundingBox
)

data class TableBlock(
    override val blockIndex: Int,
    override val sourceId: Long?,
    override val pageNumber: Int,
    override val bbox: BoundingBox,

    val rows: List<ParsedTableRow>,

    val previousTableId: Long?,
    val nextTableId: Long?
) : ParsedBlock

data class ParsedTableRow(
    val rowNumber: Int,
    val cells: List<ParsedTableCell>
)

data class ParsedTableCell(
    val rowNumber: Int,
    val columnNumber: Int,
    val rowSpan: Int,
    val columnSpan: Int,

    val text: String,
    val pageNumber: Int,
    val bbox: BoundingBox
)

data class BoundingBox(
    val left: Float,
    val bottom: Float,
    val right: Float,
    val top: Float
) {
    val width: Float
        get() = right - left

    val height: Float
        get() = top - bottom
}