package org.ai_processor.processing.chunking

import org.ai_processor.processing.pdfreader.ParsedPDF
import org.springframework.stereotype.Component
import java.util.UUID

import kotlin.collections.ArrayDeque

@Component
class Chunker(
    private val maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE,
    private val maxChunkOverlap: Int = DEFAULT_MAX_CHUNK_OVERLAP
) {

    fun chunkPDF(parsedPDF: ParsedPDF): List<PdfChunk> {
        var currentChunkSize = 0
        val chunks = mutableListOf<PdfChunk>()
        val currentTextBlocks = ArrayDeque<TextBlock>()
        var chunkIndex = 1
        for (page in parsedPDF.pages) {
            for (textBlock in page.textBlocks) {
                if (currentTextBlocks.isNotEmpty() && currentChunkSize + textBlock.text.length > maxChunkSize) {
                    chunks.add(buildPdfChunk(parsedPDF.documentId, chunkIndex, currentTextBlocks))
                    chunkIndex += 1
                    while (currentChunkSize > maxChunkOverlap) {
                        val overlap = currentTextBlocks.removeFirst()
                        currentChunkSize -= overlap.text.length
                    }
                }
                currentTextBlocks.addLast(textBlock)
                currentChunkSize += textBlock.text.length
            }
        }
        if (currentTextBlocks.isNotEmpty()) {
            chunks.add(buildPdfChunk(parsedPDF.documentId, chunkIndex, currentTextBlocks))
        }
        return chunks
    }

    private fun buildPdfChunk(documentId: UUID, chunkIndex: Int, currentTextBlocks: ArrayDeque<TextBlock>): PdfChunk {
        val text = currentTextBlocks.joinToString(separator = " ") { it.text }
        val highlightRectangles = currentTextBlocks.map {
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
            pageStart = currentTextBlocks.first().pageNumber,
            pageEnd = currentTextBlocks.last().pageNumber,
            highlightRects = highlightRectangles
        )
    }

    private companion object {
        const val DEFAULT_MAX_CHUNK_SIZE = 300
        const val DEFAULT_MAX_CHUNK_OVERLAP = 50
    }
}
