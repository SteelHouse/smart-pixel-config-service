package com.steelhouse.smartpixelconfigservice.datasource.dao

import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MultipleTables(
    private val sqlCounter: Counter,
    private val jdbcTemplate: JdbcTemplate
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun updateMultipleTables(queries: List<String>): Boolean? {
        return try {
            // Execute updates on multiple tables within a transaction
            for (query in queries) {
                jdbcTemplate.update(query)
            }
            // Commit the transaction
            true
        } catch (e: Exception) {
            // Rollback transaction if any update fails
            log.error("unknown db exception to batch update multiple tables. error message=[${e.message}]")
            sqlCounter.labels("multiple_tables", "batch_update", "error").inc()
            // Rethrow the exception so the error message can be delivered to the client
            throw e
        }
    }
}
