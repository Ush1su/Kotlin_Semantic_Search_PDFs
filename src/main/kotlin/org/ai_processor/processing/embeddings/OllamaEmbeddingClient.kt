package org.ai_processor.processing.embeddings

import org.ai_processor.processing.chunking.PdfChunk
import org.springframework.stereotype.Component
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class OllamaEmbeddingClient(
    private val properties: EmbeddingProperties,
) : EmbeddingClient {

    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl)
        .build()

    override fun embed(texts: List<String>): List<List<Float>> {
        require(texts.isNotEmpty()) {
            "At least one text is required for embedding"
        }

        require(texts.none(String::isBlank)) {
            "Embedding input must not contain blank text"
        }

        val request = OllamaEmbeddingRequest(
            model = properties.model,
            input = texts,
            truncate = properties.truncate,
            dimensions = properties.dimensions,
        )

        val response = try {
            restClient
                .post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OllamaEmbeddingResponse::class.java)
                ?: throw EmbeddingException(
                    "Ollama returned an empty response body",
                )
        } catch (exception: RestClientException) {
            throw EmbeddingException(
                message = "Failed to generate embeddings using model " +
                        properties.model,
                cause = exception,
            )
        }

        validateResponse(
            inputCount = texts.size,
            embeddings = response.embeddings,
        )

        return response.embeddings
    }

    private fun validateResponse(
        inputCount: Int,
        embeddings: List<List<Float>>,
    ) {
        check(embeddings.size == inputCount) {
            "Ollama returned ${embeddings.size} embeddings " +
                    "for $inputCount input texts"
        }

        check(embeddings.all { it.isNotEmpty() }) {
            "Ollama returned an empty embedding vector"
        }

        val dimensions = embeddings
            .map { it.size }
            .distinct()

        check(dimensions.size == 1) {
            "Ollama returned vectors with different dimensions: $dimensions"
        }
    }

}