package org.ai_processor.processing.chunking

import org.ai_processor.processing.pdfreader.BoundingBox
import org.ai_processor.processing.pdfreader.CaptionBlock
import org.ai_processor.processing.pdfreader.HeadingBlock
import org.ai_processor.processing.pdfreader.ListBlock
import org.ai_processor.processing.pdfreader.ParagraphBlock
import org.ai_processor.processing.pdfreader.ParsedListItem
import org.ai_processor.processing.pdfreader.ParsedPDF
import org.ai_processor.processing.pdfreader.ParsedTableCell
import org.ai_processor.processing.pdfreader.ParsedTableRow
import org.ai_processor.processing.pdfreader.TableBlock
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkerTest {
    private val chunker = Chunker()

    @Test
    fun `chunkPDF returns no chunks when the document has no blocks`() {
        val parsedPDF = parsedPdf(blocks = emptyList())

        assertTrue(chunker.chunkPDF(parsedPDF).isEmpty())
    }

    @Test
    fun `chunkPDF skips blocks that render to blank text`() {
        val parsedPDF = parsedPdf(
            blocks = listOf(
                paragraph(text = "   ", blockIndex = 0),
                listBlock(items = emptyList(), blockIndex = 1),
                tableBlock(rows = emptyList(), blockIndex = 2),
                paragraph(text = "Hello", blockIndex = 3)
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(1, chunks.size)
        assertEquals("Hello", chunks.single().text)
        assertEquals(1, chunks.single().highlightRects.size)
    }

    @Test
    fun `chunkPDF accumulates blocks until the size threshold is exceeded then starts a new chunk`() {
        val documentId = UUID.randomUUID()
        val chunker = Chunker(maxChunkSize = 10)
        val parsedPDF = parsedPdf(
            documentId = documentId,
            blocks = listOf(
                paragraph(text = "aaaa", blockIndex = 0, pageNumber = 1),
                paragraph(text = "bbbb", blockIndex = 1, pageNumber = 1),
                paragraph(text = "cccc", blockIndex = 2, pageNumber = 2)
            )
        )

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(listOf("aaaa\n\nbbbb", "cccc"), chunks.map { it.text })
        assertEquals(listOf(0, 1), chunks.map { it.chunkIndex })
        assertEquals(listOf(1, 2), chunks.map { it.pageStart })
        assertEquals(listOf(1, 2), chunks.map { it.pageEnd })
        assertEquals(listOf(2, 1), chunks.map { it.highlightRects.size })
        assertEquals(listOf(documentId, documentId), chunks.map { it.documentId })
    }

    @Test
    fun `chunkPDF keeps a single oversized block as its own chunk without splitting it`() {
        val chunker = Chunker(maxChunkSize = 5)
        val longText = "x".repeat(50)
        val parsedPDF = parsedPdf(blocks = listOf(paragraph(text = longText, blockIndex = 0)))

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(1, chunks.size)
        assertEquals(longText, chunks.single().text)
        assertEquals(1, chunks.single().highlightRects.size)
    }

    @Test
    fun `chunkPDF renders each block type according to its own format and maps highlight rects`() {
        val heading = heading(text = "Title", headingLevel = 1, blockIndex = 0, pageNumber = 1)
        val paragraph = paragraph(text = "Body paragraph.", blockIndex = 1, pageNumber = 1)
        val caption = caption(text = "Fig. 1", blockIndex = 2, pageNumber = 2)
        val list = listBlock(
            items = listOf(
                ParsedListItem(text = "First", pageNumber = 2, bbox = bbox(3)),
                ParsedListItem(text = "Second", pageNumber = 2, bbox = bbox(3))
            ),
            blockIndex = 3,
            pageNumber = 2
        )
        val table = tableBlock(
            rows = listOf(
                ParsedTableRow(
                    rowNumber = 0,
                    cells = listOf(
                        tableCell(text = "A", columnNumber = 0),
                        tableCell(text = "B", columnNumber = 1)
                    )
                )
            ),
            blockIndex = 4,
            pageNumber = 3
        )
        val blocks = listOf(heading, paragraph, caption, list, table)
        val parsedPDF = parsedPdf(blocks = blocks)

        val chunks = chunker.chunkPDF(parsedPDF)

        assertEquals(1, chunks.size)
        assertEquals(
            "Title\n\nBody paragraph.\n\nFig. 1\n\n- First\n- Second\n\nA | B",
            chunks.single().text
        )
        assertEquals(1, chunks.single().pageStart)
        assertEquals(3, chunks.single().pageEnd)
        assertEquals(
            blocks.map { it.pageNumber to it.bbox },
            chunks.single().highlightRects.map { it.pageNumber to it.bbox }
        )
    }

    private fun parsedPdf(
        documentId: UUID = UUID.randomUUID(),
        blocks: List<org.ai_processor.processing.pdfreader.ParsedBlock>
    ) = ParsedPDF(
        documentId = documentId,
        numberOfPages = blocks.maxOfOrNull { it.pageNumber } ?: 0,
        title = null,
        author = null,
        blocks = blocks
    )

    private fun bbox(seed: Int) = BoundingBox(
        left = seed.toFloat(),
        bottom = seed.toFloat(),
        right = seed + 10f,
        top = seed + 10f
    )

    private fun paragraph(text: String, blockIndex: Int, pageNumber: Int = 1) = ParagraphBlock(
        blockIndex = blockIndex,
        sourceId = null,
        pageNumber = pageNumber,
        bbox = bbox(blockIndex),
        text = text
    )

    private fun heading(text: String, headingLevel: Int, blockIndex: Int, pageNumber: Int = 1) = HeadingBlock(
        blockIndex = blockIndex,
        sourceId = null,
        pageNumber = pageNumber,
        bbox = bbox(blockIndex),
        text = text,
        headingLevel = headingLevel
    )

    private fun caption(text: String, blockIndex: Int, pageNumber: Int = 1) = CaptionBlock(
        blockIndex = blockIndex,
        sourceId = null,
        pageNumber = pageNumber,
        bbox = bbox(blockIndex),
        text = text,
        linkedContentId = null
    )

    private fun listBlock(items: List<ParsedListItem>, blockIndex: Int, pageNumber: Int = 1) = ListBlock(
        blockIndex = blockIndex,
        sourceId = null,
        pageNumber = pageNumber,
        bbox = bbox(blockIndex),
        items = items
    )

    private fun tableBlock(rows: List<ParsedTableRow>, blockIndex: Int, pageNumber: Int = 1) = TableBlock(
        blockIndex = blockIndex,
        sourceId = null,
        pageNumber = pageNumber,
        bbox = bbox(blockIndex),
        rows = rows,
        previousTableId = null,
        nextTableId = null
    )

    private fun tableCell(text: String, columnNumber: Int) = ParsedTableCell(
        rowNumber = 0,
        columnNumber = columnNumber,
        rowSpan = 1,
        columnSpan = 1,
        text = text,
        pageNumber = 3,
        bbox = bbox(columnNumber)
    )
}
