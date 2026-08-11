package org.ai_processor.processing.pdfreader

import org.ai_processor.processing.pdfreader.parsers.CaptionBlockParser
import org.ai_processor.processing.pdfreader.parsers.TableBlockParser
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Path
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PDFParserTest {
    private val jsonMapper = JsonMapper.builder().build()
    private val parser = PDFParser(jsonMapper)

    @Test
    fun `parse extracts document metadata and ordered heading and paragraph blocks from a real PDF`() {
        val documentId = UUID.randomUUID()

        val parsedPDF = parser.parse(testDataPath("MIT_LU_LINAL.pdf"), documentId)

        assertEquals(documentId, parsedPDF.documentId)
        assertEquals(4, parsedPDF.numberOfPages)
        assertEquals("Factorization into A = LU", parsedPDF.title)
        assertEquals("Heidi Burgiel", parsedPDF.author)
        assertTrue(parsedPDF.blocks.isNotEmpty())

        assertEquals(parsedPDF.blocks.indices.toList(), parsedPDF.blocks.map { it.blockIndex })
        assertTrue(parsedPDF.blocks.all { it.pageNumber in 1..4 })
        assertTrue(parsedPDF.blocks.all { it.bbox.width > 0f && it.bbox.height > 0f })

        val heading = parsedPDF.blocks.filterIsInstance<HeadingBlock>().first()
        assertTrue(heading.headingLevel >= 1)
        assertTrue(heading.text.isNotBlank())

        val paragraph = parsedPDF.blocks.filterIsInstance<ParagraphBlock>().first()
        assertTrue(paragraph.text.isNotBlank())
    }

    @Test
    fun `parse extracts list blocks with items from a real PDF containing lists`() {
        val parsedPDF = parser.parse(testDataPath("paper_document_encoder.pdf"), UUID.randomUUID())

        val list = parsedPDF.blocks.filterIsInstance<ListBlock>().first()

        assertTrue(list.items.isNotEmpty())
        assertTrue(list.items.all { it.text.isNotBlank() })
        assertTrue(list.items.all { it.bbox.width > 0f && it.bbox.height > 0f })
    }

    @Test
    fun `parse wraps a failure to read the input file into a PdfParsingException`() {
        val missingFile = Path.of("test_data/does-not-exist.pdf")

        val exception = kotlin.test.assertFailsWith<PdfParsingException> {
            parser.parse(missingFile, UUID.randomUUID())
        }

        assertTrue(exception.message!!.contains(missingFile.toString()))
        assertIs<Exception>(exception.cause)
    }

    @Test
    fun `TableBlockParser maps rows, cells and cross-table links from a table node`() {
        val node = jsonMapper.readTree(
            """
            {
              "type": "table",
              "id": 5,
              "page number": 2,
              "bounding box": [10.0, 20.0, 100.0, 200.0],
              "previous table id": 3,
              "rows": [
                {
                  "row number": 0,
                  "cells": [
                    {
                      "row number": 0, "column number": 0, "row span": 1, "column span": 2,
                      "page number": 2, "bounding box": [10.0, 20.0, 50.0, 200.0],
                      "kids": [
                        { "content": "Alpha" },
                        { "content": "Beta", "kids": [ { "content": "Nested" } ] }
                      ]
                    },
                    {
                      "row number": 0, "column number": 2, "row span": 1, "column span": 1,
                      "page number": 2, "bounding box": [50.0, 20.0, 100.0, 200.0],
                      "kids": []
                    }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val table = TableBlockParser.parse(node, blockIndex = 0)

        assertEquals(5L, table.sourceId)
        assertEquals(2, table.pageNumber)
        assertEquals(BoundingBox(10f, 20f, 100f, 200f), table.bbox)
        assertEquals(3L, table.previousTableId)
        assertNull(table.nextTableId)

        val cells = table.rows.single().cells
        assertEquals("Alpha Beta Nested", cells[0].text)
        assertEquals(2, cells[0].columnSpan)
        assertEquals("", cells[1].text)
        assertEquals(2, cells[1].columnNumber)
    }

    @Test
    fun `CaptionBlockParser trims content and reads an optional linked content id`() {
        val withLink = jsonMapper.readTree(
            """
            {
              "type": "caption", "id": 7, "page number": 1,
              "bounding box": [1.0, 2.0, 3.0, 4.0],
              "content": "  Figure 1: Diagram  ", "linked content id": 42
            }
            """.trimIndent()
        )
        val withoutLink = jsonMapper.readTree(
            """
            {
              "type": "caption", "page number": 1,
              "bounding box": [1.0, 2.0, 3.0, 4.0],
              "content": "Figure 2"
            }
            """.trimIndent()
        )

        val captionWithLink = CaptionBlockParser.parse(withLink, blockIndex = 0)
        val captionWithoutLink = CaptionBlockParser.parse(withoutLink, blockIndex = 1)

        assertEquals("Figure 1: Diagram", captionWithLink.text)
        assertEquals(7L, captionWithLink.sourceId)
        assertEquals(42L, captionWithLink.linkedContentId)
        assertNull(captionWithoutLink.sourceId)
        assertNull(captionWithoutLink.linkedContentId)
    }

    private fun testDataPath(fileName: String): Path =
        Path.of("test_data", fileName)
}
