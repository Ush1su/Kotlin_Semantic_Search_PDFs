package org.ai_processor.processing.pdfreader

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PDFParserTest {
    private val parser = PDFParser()

    @Test
    fun `parse extracts text blocks from a generated multi-page PDF`() {
        val documentId = UUID.randomUUID()
        val pdfPath = createPdf(
            pages = listOf(
                listOf("Alpha searchable text", "Beta second line"),
                listOf("Gamma answer paragraph")
            )
        )

        try {
            val parsedPDF = parser.parse(pdfPath, documentId)

            assertEquals(documentId, parsedPDF.documentId)
            assertEquals(2, parsedPDF.pages.size)
            assertEquals(1, parsedPDF.pages[0].pageNumber)
            assertEquals(2, parsedPDF.pages[1].pageNumber)
            assertEquals(PDRectangle.LETTER.width, parsedPDF.pages[0].width)
            assertEquals(PDRectangle.LETTER.height, parsedPDF.pages[0].height)

            assertEquals(
                listOf("Alpha searchable text", "Beta second line"),
                parsedPDF.pages[0].textBlocks.map { it.text }
            )
            assertEquals(
                listOf("Gamma answer paragraph"),
                parsedPDF.pages[1].textBlocks.map { it.text }
            )

            val firstBlock = parsedPDF.pages[0].textBlocks.first()
            assertEquals(0, firstBlock.blockIndex)
            assertEquals(1, firstBlock.pageNumber)
            assertTrue(firstBlock.bbox.x > 0f)
            assertTrue(firstBlock.bbox.y > 0f)
            assertTrue(firstBlock.bbox.width > 0f)
            assertTrue(firstBlock.bbox.height > 0f)
        } finally {
            Files.deleteIfExists(pdfPath)
        }
    }

    @Test
    fun `parse preserves caller supplied document id for an empty real PDF`() {
        val documentId = UUID.randomUUID()
        val pdfPath = createPdf(pages = listOf(emptyList()))

        try {
            val parsedPDF = parser.parse(pdfPath, documentId)

            assertEquals(documentId, parsedPDF.documentId)
            assertEquals(1, parsedPDF.pages.size)
            assertTrue(parsedPDF.pages.single().textBlocks.isEmpty())
        } finally {
            Files.deleteIfExists(pdfPath)
        }
    }

    private fun createPdf(pages: List<List<String>>): Path {
        val pdfPath = Files.createTempFile("ai-pdf-processor-", ".pdf")
        val font = PDType1Font(Standard14Fonts.FontName.HELVETICA)

        PDDocument().use { document ->
            pages.forEach { pageLines ->
                val page = PDPage(PDRectangle.LETTER)
                document.addPage(page)

                if (pageLines.isNotEmpty()) {
                    PDPageContentStream(document, page).use { content ->
                        content.beginText()
                        content.setFont(font, 12f)
                        content.newLineAtOffset(72f, 720f)

                        pageLines.forEachIndexed { index, line ->
                            if (index > 0) {
                                content.newLineAtOffset(0f, -18f)
                            }
                            content.showText(line)
                        }

                        content.endText()
                    }
                }
            }
            document.save(pdfPath.toFile())
        }

        return pdfPath
    }
}
