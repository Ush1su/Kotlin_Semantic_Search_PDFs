package org.ai_processor.processing.pdfreader.parsers

import org.ai_processor.processing.pdfreader.HeadingBlock
import org.ai_processor.processing.pdfreader.OdlJson
import tools.jackson.databind.JsonNode

internal object HeadingBlockParser : BlockParser {

    override val nodeType = OdlJson.HEADING

    override fun parse(node: JsonNode, blockIndex: Int) = HeadingBlock(
        blockIndex = blockIndex,
        sourceId = node.sourceId(),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox(),
        text = node.content(),
        headingLevel = node.requiredInt(OdlJson.HEADING_LEVEL)
    )
}
