package com.steelhouse.smartpixelconfigservice.config

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.prometheus.client.Counter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
@Configuration
class MetricsConfig {
    @Bean
    fun sqlCounter(meterRegistry: PrometheusMeterRegistry) = Counter.Builder()
        .name("sql_counter")
        .help("sql action counter")
        .labelNames("table", "action", "result")
        .register(meterRegistry.prometheusRegistry)!!
}
