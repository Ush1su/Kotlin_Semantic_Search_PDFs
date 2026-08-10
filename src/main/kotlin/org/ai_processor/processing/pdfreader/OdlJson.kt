package org.ai_processor.processing.pdfreader

/**
 * Node types and field names used by the OpenDataLoader JSON output.
 */
internal object OdlJson {

    const val PARAGRAPH = "paragraph"
    const val HEADING = "heading"
    const val CAPTION = "caption"
    const val LIST = "list"
    const val TABLE = "table"

    const val TEXT_BLOCK = "text block"

    const val TYPE = "type"
    const val ID = "id"
    const val KIDS = "kids"
    const val CONTENT = "content"
    const val PAGE_NUMBER = "page number"
    const val BOUNDING_BOX = "bounding box"

    const val NUMBER_OF_PAGES = "number of pages"
    const val TITLE = "title"
    const val AUTHOR = "author"

    const val HEADING_LEVEL = "heading level"
    const val LINKED_CONTENT_ID = "linked content id"

    const val LIST_ITEMS = "list items"

    const val ROWS = "rows"
    const val CELLS = "cells"
    const val ROW_NUMBER = "row number"
    const val COLUMN_NUMBER = "column number"
    const val ROW_SPAN = "row span"
    const val COLUMN_SPAN = "column span"
    const val PREVIOUS_TABLE_ID = "previous table id"
    const val NEXT_TABLE_ID = "next table id"
}
