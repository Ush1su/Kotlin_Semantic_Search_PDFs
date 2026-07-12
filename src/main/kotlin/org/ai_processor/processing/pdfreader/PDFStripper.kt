package org.ai_processor.processing.pdfreader

import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.text.TextPosition

internal class PDFStripper (
    private val pageNumber: Int
) : PDFTextStripper() {
    val textPieces = mutableListOf<RawTextPiece>()

    init {
        startPage = pageNumber
        endPage = pageNumber
        sortByPosition = true
    }

    override fun writeString(
        text: String,
        textPositions: MutableList<TextPosition>,
    ) {
        super.writeString(text, textPositions)
        for (textPosition in textPositions) {
            textPieces.add(
                RawTextPiece(
                    value = textPosition.unicode,
                    pageNumber = pageNumber,
                    x = textPosition.xDirAdj,
                    y = textPosition.yDirAdj,
                    width = textPosition.widthDirAdj,
                    height = textPosition.heightDir
                )
            )
        }
    }
}