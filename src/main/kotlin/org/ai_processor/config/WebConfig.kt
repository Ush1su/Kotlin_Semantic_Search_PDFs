package org.ai_processor.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.method.HandlerTypePredicate

/**
 * Puts every REST controller under /api.
 *
 * The prefix lives here rather than in server.servlet.context-path so that the
 * root path stays free for the built frontend, which Spring serves from the
 * classpath as static resources. API and UI therefore share one origin and the
 * browser never needs CORS.
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            API_PREFIX,
            HandlerTypePredicate.forAnnotation(RestController::class.java)
        )
    }

    private companion object {
        const val API_PREFIX = "/api"
    }
}
