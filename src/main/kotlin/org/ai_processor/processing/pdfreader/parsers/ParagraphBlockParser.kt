package org.ai_processor.processing.pdfreader.parsers

import org.ai_processor.processing.pdfreader.OdlJson
import org.ai_processor.processing.pdfreader.ParagraphBlock
import tools.jackson.databind.JsonNode

internal object ParagraphBlockParser : BlockParser {

    override val nodeType = OdlJson.PARAGRAPH

    override fun parse(node: JsonNode, blockIndex: Int) = ParagraphBlock(
        blockIndex = blockIndex,
        sourceId = node.sourceId(),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox(),
        text = node.content()
    )
}
