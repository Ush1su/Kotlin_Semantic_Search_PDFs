package org.ai_processor.document.api

import org.ai_processor.document.api.model.DocumentSearchResponse
import org.ai_processor.document.services.DocumentSearchService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search")
class DocumentSearchController(
    private val documentSearchService: DocumentSearchService
) {

    @GetMapping
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(required = false) minimumScore: Float?
    ): DocumentSearchResponse {
        return documentSearchService.search(
            query = query,
            limit = limit,
            minimumScore = minimumScore
        )
    }
}