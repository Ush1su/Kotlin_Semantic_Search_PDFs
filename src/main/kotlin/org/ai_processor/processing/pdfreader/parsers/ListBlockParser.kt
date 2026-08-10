package org.ai_processor.processing.pdfreader.parsers

import org.ai_processor.processing.pdfreader.ListBlock
import org.ai_processor.processing.pdfreader.OdlJson
import org.ai_processor.processing.pdfreader.ParsedListItem
import tools.jackson.databind.JsonNode

internal object ListBlockParser : BlockParser {

    override val nodeType = OdlJson.LIST

    override fun parse(node: JsonNode, blockIndex: Int) = ListBlock(
        blockIndex = blockIndex,
        sourceId = node.sourceId(),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox(),
        items = node[OdlJson.LIST_ITEMS]?.map { parseItem(it) } ?: emptyList()
    )

    private fun parseItem(node: JsonNode) = ParsedListItem(
        text = node.content(),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox()
    )
}
