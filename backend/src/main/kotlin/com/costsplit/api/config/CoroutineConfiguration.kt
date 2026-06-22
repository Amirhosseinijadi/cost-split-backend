package com.costsplit.api.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoroutineConfiguration {
    @Bean("databaseDispatcher")
    fun databaseDispatcher(
        @Value("\${app.coroutines.database-parallelism}") parallelism: Int,
    ): CoroutineDispatcher = Dispatchers.IO.limitedParallelism(parallelism)
}

