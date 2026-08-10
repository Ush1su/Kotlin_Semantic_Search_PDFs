package org.ai_processor.processing.pdfreader.parsers

import org.ai_processor.processing.pdfreader.BoundingBox
import org.ai_processor.processing.pdfreader.OdlJson
import org.ai_processor.processing.pdfreader.ParsedBlock
import org.ai_processor.processing.pdfreader.PdfParsingException
import tools.jackson.databind.JsonNode

/**
 * Parses one OpenDataLoader node into a [ParsedBlock].
 * An implementation declares the node type it handles and maps the node onto a
 * block. The field reads every implementation needs are provided here, so a
 * missing or malformed field is reported the same way everywhere.
 */
internal interface BlockParser {
    val nodeType: String

    fun parse(node: JsonNode, blockIndex: Int): ParsedBlock

    fun JsonNode.sourceId(): Long? = optionalLong(OdlJson.ID)

    fun JsonNode.pageNumber(): Int = requiredInt(OdlJson.PAGE_NUMBER)

    fun JsonNode.content(): String =
        requiredField(OdlJson.CONTENT).asString().trim()

    fun JsonNode.requiredInt(name: String): Int =
        requiredField(name).asInt()

    fun JsonNode.optionalLong(name: String): Long? =
        this[name]?.takeUnless { it.isNull }?.asLong()

    fun JsonNode.boundingBox(): BoundingBox {
        val coordinates = requiredField(OdlJson.BOUNDING_BOX)

        if (!coordinates.isArray || coordinates.size() < BOUNDING_BOX_SIZE) {
            throw PdfParsingException(
                "Malformed '${OdlJson.BOUNDING_BOX}' in ${describe()}"
            )
        }

        return BoundingBox(
            left = coordinates[0].floatValue(),
            bottom = coordinates[1].floatValue(),
            right = coordinates[2].floatValue(),
            top = coordinates[3].floatValue()
        )
    }

    private fun JsonNode.requiredField(name: String): JsonNode =
        this[name] ?: throw PdfParsingException("Missing '$name' in ${describe()}")

    private fun JsonNode.describe(): String {
        val type = this[OdlJson.TYPE]?.asString() ?: "unknown"
        val page = this[OdlJson.PAGE_NUMBER]?.asInt()

        return if (page == null) "'$type' node" else "'$type' node on page $page"
    }

    private companion object {
        const val BOUNDING_BOX_SIZE = 4
    }
}
