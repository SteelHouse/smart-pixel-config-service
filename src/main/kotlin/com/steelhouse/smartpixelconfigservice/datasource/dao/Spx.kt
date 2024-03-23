package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.getSpxListFieldQueryInfoString
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.Statement

@Component
class Spx(
    private val sqlCounter: Counter,
    private val jdbcTemplate: JdbcTemplate,
    private val spxRepository: SpxRepository
) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Cacheable("spxListByAidCache")
    fun getSpxListByAdvertiserId(advertiserId: Int): List<AdvertiserSmartPxVariables>? {
        return queryDbForSpxList(createSqlQueryToGetByAdvertiserId(advertiserId))
    }

    fun getSpxListByAdvertiserId(advertiserId: Int, trpxCallParameterDefaultsIdList: List<String>): List<AdvertiserSmartPxVariables>? {
        var sqlQuery = createSqlQueryToGetByAdvertiserId(advertiserId)
        if (trpxCallParameterDefaultsIdList.isNotEmpty()) sqlQuery += " AND trpx_call_parameter_defaults_id IN (" + trpxCallParameterDefaultsIdList.joinToString() + ")"
        return queryDbForSpxList(sqlQuery)
    }

    private fun createSqlQueryToGetByAdvertiserId(advertiserId: Int): String {
        return """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE advertiser_id = '$advertiserId'
        """.trimIndent()
    }

    @Cacheable("spxListByFieldQueryCache")
    fun getSpxListByFieldQueryKeyword(keyword: String): List<AdvertiserSmartPxVariables>? {
        return queryDbForSpxList(createSqlQueryToGetByFieldQueryKeyword(keyword))
    }

    private fun createSqlQueryToGetByFieldQueryKeyword(keyword: String): String {
        return """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE query ILIKE '%$keyword%'
        """.trimIndent()
    }

    private fun queryDbForSpxList(sql: String): List<AdvertiserSmartPxVariables>? {
        log.debug("sql=[\n$sql\n]")
        try {
            return jdbcTemplate.query(sql, BeanPropertyRowMapper(AdvertiserSmartPxVariables::class.java))
        } catch (e: EmptyResultDataAccessException) {
            log.debug("no spx found in db. sql=[$sql]")
            return emptyList()
        } catch (e: Exception) {
            log.error("unknown db exception to get data. sql=[$sql]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "select", "error").inc()
        }
        return null
    }

    /**
     * Returns the rows of advertiser_smart_px_variables updated.
     *
     * @return empty list for unknown db exception. No recovery is needed.
     *         list of rows for successful db transaction.
     *         null for db error. Further recovery is needed.
     */
    fun batchUpdateSpxListBySqlQueryAndReturnRows(list: List<AdvertiserSmartPxVariables>, sql: String): List<Map<String?, Any?>>? {
        val listSize = list.size
        val logString = getSpxListFieldQueryInfoString(list)
        val rows: List<Map<String?, Any?>>
        try {
            rows = batchUpdateBySqlQueryAndReturnRows(list, sql)
        } catch (e: Exception) {
            log.error("unknown db exception to batch update. error message=[${e.message}]; $logString")
            sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "error").inc()
            return emptyList()
        }
        val resultSize = rows.size
        return if (rows.size == listSize) {
            log.debug("$resultSize spx have been created")
            sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "ok").inc()
            rows
        } else {
            log.error("problems with db batch update: recovery needed. returned result=[$resultSize]; $logString")
            sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "error").inc()
            null
        }
    }

    /**
     * Uses the input sql query to update list of AdvertiserSmartPxVariables(SPX/pixel) and returns the
     * rows updated.
     *
     * The input AdvertiserSmartPxVariables must have the following fields:
     * - advertiserId
     * - query
     *
     */
    @Transactional
    fun batchUpdateBySqlQueryAndReturnRows(list: List<AdvertiserSmartPxVariables>, sql: String): List<Map<String?, Any?>> {
        val rows = mutableListOf<Map<String?, Any?>>()
        for (spx in list) {
            val keyHolder = GeneratedKeyHolder()
            jdbcTemplate.update({ connection ->
                val ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                ps.setInt(1, spx.advertiserId)
                ps.setString(2, spx.query)
                ps
            }, keyHolder)

            val generatedSpx = keyHolder.keyList
            if (generatedSpx.isNotEmpty()) {
                log.debug("generatedSpx=[{}]", generatedSpx)
                rows.add(generatedSpx[0])
            }
        }
        return rows
    }

    fun getSpxListByVariableIds(variableIds: List<Int>): List<AdvertiserSmartPxVariables>? {
        val ids = variableIds.joinToString()
        return try {
            spxRepository.findAllById(variableIds).toList()
        } catch (e: Exception) {
            log.error("unknown db exception to get data. variableIds=[$ids]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "select", "error").inc()
            null
        }
    }

    fun deleteSPXsByVariableIds(variableIds: List<Int>): Boolean {
        val ids = variableIds.joinToString()
        return try {
            spxRepository.deleteAllById(variableIds)
            log.debug("spx deletion succeed. variableIds=[$ids]")
            sqlCounter.labels("advertiser_smart_px_variables", "delete", "ok").inc()
            true
        } catch (e: Exception) {
            log.error("unknown db exception to delete data. variableIds=[$ids]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "delete", "error").inc()
            false
        }
    }
}
