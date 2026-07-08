package org.ai_processor.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.ai_processor.chunking.HighlightRect
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(
    name = "document_chunks",
    indexes = [
        Index(name = "idx_document_chunks_document_id", columnList = "document_id"),
        Index(name = "idx_document_chunks_document_id_chunk_index", columnList = "document_id, chunk_index")
    ]
)
class DocumentChunkEntity(
    @Id
    val id : UUID,

    @Column(name = "document_id", nullable = false)
    val documentId: UUID,

    @Column(name = "chunk_index", nullable = false)
    val chunkIndex: Int,

    @Column(columnDefinition = "TEXT", nullable = false)
    val text: String,

    @Column(name = "page_start", nullable = false)
    val pageStart: Int,

    @Column(name = "page_end", nullable = false)
    val pageEnd: Int,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "highlight_rects", columnDefinition = "jsonb")
    val highlightRects: List<HighlightRect> = emptyList()
)