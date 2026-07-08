package org.ai_processor.chunking

import org.ai_processor.pdfreader.ParsedPDF
import org.ai_processor.pdfreader.TextBlock
import org.springframework.stereotype.Component
import java.util.UUID

import kotlin.collections.ArrayDeque

@Component
class Chunker {
    private val maxChunkSize = 1000
    private val maxChunkOverlap = 200

    fun chunkPDF(parsedPDF: ParsedPDF): List<PdfChunk> {
        var currentChunkSize = 0
        val chunks = mutableListOf<PdfChunk>()
        val currentTextBlocks = ArrayDeque<TextBlock>()
        var chunkIndex = 1
        for (page in parsedPDF.pages) {
            for (textBlock in page.textBlocks) {
                if (currentChunkSize + textBlock.text.length > maxChunkSize) {
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
        chunks.add(buildPdfChunk(parsedPDF.documentId, chunkIndex, currentTextBlocks))
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
}