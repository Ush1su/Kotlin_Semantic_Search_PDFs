package org.ai_processor.processing.chunking

import org.ai_processor.processing.pdfreader.CaptionBlock
import org.ai_processor.processing.pdfreader.HeadingBlock
import org.ai_processor.processing.pdfreader.ListBlock
import org.ai_processor.processing.pdfreader.ParagraphBlock
import org.ai_processor.processing.pdfreader.ParsedBlock
import org.ai_processor.processing.pdfreader.ParsedPDF
import org.ai_processor.processing.pdfreader.TableBlock
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class Chunker(
    private val maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE
) {

    fun chunkPDF(
        parsedPDF: ParsedPDF
    ): List<PdfChunk> {

        val chunks = mutableListOf<PdfChunk>()
        val currentBlocks = mutableListOf<ParsedBlock>()

        var currentSize = 0

        for (block in parsedPDF.blocks) {
            val blockText = block.toChunkText()

            if (blockText.isBlank()) {
                continue
            }

            if (currentBlocks.isNotEmpty() && currentSize + blockText.length > maxChunkSize) {
                chunks += buildPdfChunk(
                    documentId = parsedPDF.documentId,
                    chunkIndex = chunks.size,
                    blocks = currentBlocks
                )

                currentBlocks.clear()
                currentSize = 0
            }

            currentBlocks += block
            currentSize += blockText.length
        }

        if (currentBlocks.isNotEmpty()) {
            chunks += buildPdfChunk(
                documentId = parsedPDF.documentId,
                chunkIndex = chunks.size,
                blocks = currentBlocks
            )
        }

        return chunks
    }

    private fun buildPdfChunk(
        documentId: UUID,
        chunkIndex: Int,
        blocks: List<ParsedBlock>
    ): PdfChunk {

        val text = blocks.joinToString("\n\n") {
            it.toChunkText()
        }

        val highlightRects = blocks.map {
            HighlightRect(
                pageNumber = it.pageNumber,
                bbox = it.bbox
            )
        }

        return PdfChunk(
            id = UUID.randomUUID(),
            documentId = documentId,
            chunkIndex = chunkIndex,
            text = text,
            pageStart = blocks.first().pageNumber,
            pageEnd = blocks.last().pageNumber,
            highlightRects = highlightRects
        )
    }

    private companion object {
        const val DEFAULT_MAX_CHUNK_SIZE = 500
    }
}

private fun ParsedBlock.toChunkText(): String =
    when (this) {
        is ParagraphBlock ->
            text

        is HeadingBlock ->
            text

        is CaptionBlock ->
            text

        is ListBlock ->
            items.joinToString("\n") { "- ${it.text}" }

        is TableBlock ->
            rows.joinToString("\n") { row ->
                row.cells.joinToString(" | ") { cell ->
                    cell.text
                }
            }
    }
