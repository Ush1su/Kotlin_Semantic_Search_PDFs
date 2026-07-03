package org.ai_processor.pdfreader

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.text.PDFTextStripper
import java.nio.file.Path
import java.util.UUID

class PDFParser {
    fun parse(filepath: Path): ParsedPDF {
        val pages = mutableListOf<ParsedPage>()
        Loader.loadPDF(filepath.toFile()).use { document ->
            for (pageIndex in 0 until document.numberOfPages) {
                val pageNumber = pageIndex + 1
                val page: PDPage = document.getPage(pageIndex)
                val mediaBox = page.mediaBox
                val width = mediaBox.width
                val height = mediaBox.height
                val stripper = PDFStripper(pageNumber)
                val text = stripper.getText(document)
                val rawPieces = stripper.textPieces
                pages.add(buildParsedPage(pageNumber, width, height, text, rawPieces))
            }
        }
        return ParsedPDF(
            documentId = UUID.randomUUID(),
            pages = pages
        )
    }

    private fun buildParsedPage(pageNumber: Int, width: Float, height: Float, text: String, rawPieces: List<RawTextPiece>) : ParsedPage{
        val rawLines: Map<Float, List<RawTextPiece>> = rawPieces
            .groupBy { it.y }
            .toSortedMap()
            .mapValues { (_, pieces) ->
                pieces.sortedBy { it.x }
            }

        val textBlocks: MutableList<TextBlock> = mutableListOf()
        rawLines.entries.forEachIndexed { index, (y, pieces) ->
            val text = pieces.joinToString(separator = "") { it.value }
            val lineWidth: Float = pieces.fold(0f) { acc, piece ->
                acc + piece.x
            }
            val lineHeight = pieces.maxOf { it.height }
            val bbox = BoundingBox(pieces[0].x, y, lineWidth, lineHeight)
            textBlocks.add(TextBlock(
                blockIndex = index,
                pageNumber = pageNumber,
                text = text,
                bbox = bbox
            ))
        }

        return ParsedPage(
            pageNumber = pageNumber,
            width = width,
            height = height,
            textBlocks = textBlocks
        )
    }
}