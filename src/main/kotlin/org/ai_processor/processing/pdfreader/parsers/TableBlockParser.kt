package org.ai_processor.processing.pdfreader.parsers

import org.ai_processor.processing.pdfreader.OdlJson
import org.ai_processor.processing.pdfreader.ParsedTableCell
import org.ai_processor.processing.pdfreader.ParsedTableRow
import org.ai_processor.processing.pdfreader.TableBlock
import tools.jackson.databind.JsonNode

internal object TableBlockParser : BlockParser {

    override val nodeType = OdlJson.TABLE

    override fun parse(node: JsonNode, blockIndex: Int) = TableBlock(
        blockIndex = blockIndex,
        sourceId = node.sourceId(),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox(),
        rows = node[OdlJson.ROWS]?.map { parseRow(it) } ?: emptyList(),
        previousTableId = node.optionalLong(OdlJson.PREVIOUS_TABLE_ID),
        nextTableId = node.optionalLong(OdlJson.NEXT_TABLE_ID)
    )

    private fun parseRow(node: JsonNode) = ParsedTableRow(
        rowNumber = node.requiredInt(OdlJson.ROW_NUMBER),
        cells = node[OdlJson.CELLS]?.map { parseCell(it) } ?: emptyList()
    )

    private fun parseCell(node: JsonNode) = ParsedTableCell(
        rowNumber = node.requiredInt(OdlJson.ROW_NUMBER),
        columnNumber = node.requiredInt(OdlJson.COLUMN_NUMBER),
        rowSpan = node.requiredInt(OdlJson.ROW_SPAN),
        columnSpan = node.requiredInt(OdlJson.COLUMN_SPAN),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox(),
        text = cellText(node)
    )

    /**
     * A cell has no `content` of its own, its text sits in nested `kids`, so
     * every descendant is collected in reading order.
     */
    private fun cellText(node: JsonNode): String =
        node[OdlJson.KIDS]
            ?.flatMap { textSegments(it) }
            ?.joinToString(separator = " ")
            ?: ""

    private fun textSegments(node: JsonNode): List<String> {
        val ownText = node[OdlJson.CONTENT]
            ?.asString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(::listOf)
            ?: emptyList()

        val childrenText = node[OdlJson.KIDS]
            ?.flatMap { textSegments(it) }
            ?: emptyList()

        return ownText + childrenText
    }
}
