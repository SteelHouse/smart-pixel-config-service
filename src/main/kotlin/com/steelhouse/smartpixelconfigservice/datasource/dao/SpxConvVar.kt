package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.SpxConversionVariables
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class SpxConvVar(
    private val sqlCounter: Counter,
    private val jdbcTemplate: JdbcTemplate
) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    fun getSpxConvVarListByAdvertiserId(advertiserId: Int, variableIdList: List<String>): List<SpxConversionVariables>? {
        var sqlQuery = createSqlQueryToGetByAdvertiserId(advertiserId)
        if (variableIdList.isNotEmpty()) sqlQuery += " AND variable_id IN (" + variableIdList.joinToString() + ")"
        return queryDbForSpxConvVarList(sqlQuery)
    }

    private fun createSqlQueryToGetByAdvertiserId(advertiserId: Int): String {
        return """
            SELECT *
            FROM spx_conversion_variables
            WHERE advertiser_id = '$advertiserId'
        """.trimIndent()
    }

    private fun queryDbForSpxConvVarList(sql: String): List<SpxConversionVariables>? {
        log.debug("sql=[\n$sql\n]")
        try {
            return jdbcTemplate.query(sql, BeanPropertyRowMapper(SpxConversionVariables::class.java))
        } catch (e: EmptyResultDataAccessException) {
            log.debug("no data found in db. sql=[$sql]")
            return emptyList()
        } catch (e: Exception) {
            log.error("unknown db exception to get data. sql=[$sql]; error message=[${e.message}]")
            sqlCounter.labels("spx_conversion_variables", "select", "error").inc()
        }
        return null
    }
}
