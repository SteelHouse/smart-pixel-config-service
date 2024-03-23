package com.steelhouse.smartpixelconfigservice.datasource

import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MultipleTablesData(
    private val sqlCounter: Counter,
    private val jdbcTemplate: JdbcTemplate
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Updates multiple tables by list of queries and returns a Status object.
     * - Status.isExecuted: indicates whether the update has been executed on database
     * - Status.numOfRowsMatched: indicates number of rows matched for update
     * - Status.message: describes the update
     */
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
            return Status(true, totalRowsMatched, null)
        } catch (e: Exception) {
            // Rollback transaction if any update fails
            log.error("unknown db exception to batch update multiple tables. error message=[${e.message}]")
            sqlCounter.labels("multiple_tables", "batch_update", "error").inc()
            return Status(false, 0, e.message)
        }
    }
}

data class Status(val isExecuted: Boolean, val numOfRowsMatched: Int, var message: String?)
