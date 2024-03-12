package com.steelhouse.smartpixelconfigservice.service

import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DataService(
    private val sqlCounter: Counter,
    private val jdbcTemplate: JdbcTemplate
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun updateMultipleTables(queries: List<String>): Status {
        var totalRowsMatched = 0
        try {
            // Execute updates on multiple tables within a transaction
            for (query in queries) {
                val rowsMatched = jdbcTemplate.update(query)
                log.debug("$rowsMatched rows matched for sql=[\n$query\n]")
                totalRowsMatched += rowsMatched
            }
            // Commit the transaction
            log.debug("total $totalRowsMatched rows matched")
            sqlCounter.labels("multiple_tables", "batch_update", "ok").inc()
            return if (totalRowsMatched > 0) {
                Status(true, null)
            } else {
                Status(true, "nothing to update")
            }
        } catch (e: Exception) {
            // Rollback transaction if any update fails
            log.error("unknown db exception to batch update multiple tables. error message=[${e.message}]")
            sqlCounter.labels("multiple_tables", "batch_update", "error").inc()
            return Status(false, e.message)
        }
    }
}

data class Status(val isSuccess: Boolean, val message: String?)
