package org.ai_processor.config

import org.ai_processor.processing.embeddings.config.EmbeddingProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean("documentProcessingExecutor")
    fun documentProcessingExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 3
            maxPoolSize = 5
            queueCapacity = 20
            keepAliveSeconds = 60

            setThreadNamePrefix("document-processing-")

            setRejectedExecutionHandler(
                ThreadPoolExecutor.AbortPolicy()
            )

            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)

            initialize()
        }
    }

    @Bean("embeddingBatchExecutor")
    fun embeddingBatchExecutor(
        properties: EmbeddingProperties
    ): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = properties.maxConcurrentBatches
            maxPoolSize = properties.maxConcurrentBatches
            queueCapacity = properties.maxConcurrentBatches
            keepAliveSeconds = 60

            setThreadNamePrefix("embedding-batch-")

            setRejectedExecutionHandler(
                ThreadPoolExecutor.AbortPolicy()
            )

            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)

            initialize()
        }
    }

    @Bean("searchExecutor")
    fun searchExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 20
            keepAliveSeconds = 60

            setThreadNamePrefix("search-")

            setRejectedExecutionHandler(
                ThreadPoolExecutor.AbortPolicy()
            )

            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)

            initialize()
        }
    }
}
