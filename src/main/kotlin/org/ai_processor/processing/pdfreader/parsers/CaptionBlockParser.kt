package org.ai_processor.processing.pdfreader.parsers

import org.ai_processor.processing.pdfreader.CaptionBlock
import org.ai_processor.processing.pdfreader.OdlJson
import tools.jackson.databind.JsonNode

internal object CaptionBlockParser : BlockParser {

    override val nodeType = OdlJson.CAPTION

    override fun parse(node: JsonNode, blockIndex: Int) = CaptionBlock(
        blockIndex = blockIndex,
        sourceId = node.sourceId(),
        pageNumber = node.pageNumber(),
        bbox = node.boundingBox(),
        text = node.content(),
        linkedContentId = node.optionalLong(OdlJson.LINKED_CONTENT_ID)
    )
}
