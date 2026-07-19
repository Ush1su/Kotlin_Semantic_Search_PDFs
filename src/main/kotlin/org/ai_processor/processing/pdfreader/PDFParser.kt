package org.ai_processor.processing.pdfreader

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDPage
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.UUID
import kotlin.math.abs

@Service
class PDFParser {
    fun parse(filepath: Path, documentId: UUID): ParsedPDF {
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
                pages.add(buildParsedPage(pageNumber, width, height, rawPieces))
            }
        }
        return ParsedPDF(
            documentId = documentId,
            pages = pages
        )
    }

    private fun buildParsedPage(
        pageNumber: Int,
        width: Float,
        height: Float,
        rawPieces: List<RawTextPiece>
    ) : ParsedPage{
        val rawLines: List<List<RawTextPiece>> = groupByApproximateY(rawPieces, tolerance = 2.0f)

        val textBlocks = rawLines.mapIndexed { index, pieces ->
            val text = pieces.joinToString(separator = "") { it.value }

            val minX = pieces.minOf { it.x }
            val maxX = pieces.maxOf { it.x + it.width }
            val minY = pieces.minOf { it.y }
            val maxY = pieces.maxOf { it.y + it.height }
            val bbox = BoundingBox(
                x = minX,
                y = minY,
                width = maxX - minX,
                height = maxY - minY
            )
            TextBlock(
                blockIndex = index,
                pageNumber = pageNumber,
                text = text,
                bbox = bbox
            )
        }

        return ParsedPage(
            pageNumber = pageNumber,
            width = width,
            height = height,
            textBlocks = textBlocks
        )
    }

    private fun groupByApproximateY(
        pieces: List<RawTextPiece>,
        tolerance: Float = 2.0f
    ): List<List<RawTextPiece>> {
        val sorted = pieces.sortedWith(compareBy<RawTextPiece> { it.y }.thenBy { it.x })

        val lines = mutableListOf<MutableList<RawTextPiece>>()

        for (piece in sorted) {
            val line = lines.firstOrNull { existingLine ->
                abs(existingLine.first().y - piece.y) <= tolerance
            }

            if (line != null) {
                line.add(piece)
            } else {
                lines.add(mutableListOf(piece))
            }
        }

        return lines.map { line ->
            line.sortedBy { it.x }
        }
    }
}
