package org.ai_processor.processing.chunking

import org.ai_processor.processing.pdfreader.BoundingBox
import org.ai_processor.processing.pdfreader.ParsedPDF
import org.ai_processor.processing.pdfreader.ParsedPage
import org.ai_processor.processing.pdfreader.TextBlock
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChunkerTest {
    private val chunker = Chunker()

    @Test
    fun `chunkPDF returns no chunks for a PDF without text blocks`() {
        val parsedPDF = ParsedPDF(
            documentId = UUID.randomUUID(),
            pages = listOf(
                ParsedPage(
                    pageNumber = 1,
                    width = 612f,
                    height = 792f,
                    textBlocks = emptyList()
                )
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `chunkPDF uses the parsed document id for every chunk`() {
        val documentId = UUID.randomUUID()
        val parsedPDF = ParsedPDF(
            documentId = documentId,
            pages = listOf(
                parsedPage(
                    textBlocks = listOf(
                        textBlock("First searchable line", blockIndex = 0),
                        textBlock("Second searchable line", blockIndex = 1)
                    )
                )
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(listOf(documentId), chunks.map { it.documentId }.distinct())
    }

    @Test
    fun `chunkPDF creates a valid chunk when one text block is larger than max chunk size`() {
        val documentId = UUID.randomUUID()
        val longText = "x".repeat(1_100)
        val parsedPDF = ParsedPDF(
            documentId = documentId,
            pages = listOf(
                parsedPage(textBlocks = listOf(textBlock(longText, blockIndex = 0)))
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(1, chunks.size)
        assertEquals(documentId, chunks.single().documentId)
        assertEquals(longText, chunks.single().text)
        assertEquals(1, chunks.single().pageStart)
        assertEquals(1, chunks.single().pageEnd)
        assertEquals(1, chunks.single().highlightRects.size)
    }

    @Test
    fun `chunkPDF keeps configured text-block overlap between chunks`() {
        val documentId = UUID.randomUUID()
        val chunker = Chunker(maxChunkSize = 10, maxChunkOverlap = 4)
        val parsedPDF = ParsedPDF(
            documentId = documentId,
            pages = listOf(
                parsedPage(
                    pageNumber = 1,
                    textBlocks = listOf(
                        textBlock("aaaa", blockIndex = 0, pageNumber = 1),
                        textBlock("bbbb", blockIndex = 1, pageNumber = 1)
                    )
                ),
                parsedPage(
                    pageNumber = 2,
                    textBlocks = listOf(
                        textBlock("cccc", blockIndex = 2, pageNumber = 2),
                        textBlock("dddd", blockIndex = 3, pageNumber = 2)
                    )
                )
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(listOf("aaaa bbbb", "bbbb cccc", "cccc dddd"), chunks.map { it.text })
        assertEquals(listOf(1, 2, 3), chunks.map { it.chunkIndex })
        assertEquals(listOf(1, 1, 2), chunks.map { it.pageStart })
        assertEquals(listOf(1, 2, 2), chunks.map { it.pageEnd })
        assertEquals(
            listOf(listOf(1, 1), listOf(1, 2), listOf(2, 2)),
            chunks.map { chunk -> chunk.highlightRects.map { it.pageNumber } }
        )
    }

    @Test
    fun `chunkPDF removes all previous text blocks when overlap is zero`() {
        val chunker = Chunker(maxChunkSize = 10, maxChunkOverlap = 0)
        val parsedPDF = ParsedPDF(
            documentId = UUID.randomUUID(),
            pages = listOf(
                parsedPage(
                    textBlocks = listOf(
                        textBlock("aaaa", blockIndex = 0),
                        textBlock("bbbb", blockIndex = 1),
                        textBlock("cccc", blockIndex = 2),
                        textBlock("dddd", blockIndex = 3)
                    )
                )
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(listOf("aaaa bbbb", "cccc dddd"), chunks.map { it.text })
        assertEquals(listOf(2, 2), chunks.map { it.highlightRects.size })
    }

    private fun parsedPage(
        pageNumber: Int = 1,
        textBlocks: List<TextBlock>
    ): ParsedPage {
        return ParsedPage(
            pageNumber = pageNumber,
            width = 612f,
            height = 792f,
            textBlocks = textBlocks
        )
    }

    private fun textBlock(
        text: String,
        blockIndex: Int,
        pageNumber: Int = 1
    ): TextBlock {
        return TextBlock(
            blockIndex = blockIndex,
            text = text,
            pageNumber = pageNumber,
            bbox = BoundingBox(
                x = blockIndex.toFloat(),
                y = blockIndex.toFloat(),
                width = 100f,
                height = 12f
            )
        )
    }
}
