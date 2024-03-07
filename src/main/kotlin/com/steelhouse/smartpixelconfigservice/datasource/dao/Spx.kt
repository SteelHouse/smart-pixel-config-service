package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.getSpxListFieldQueryInfoString
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

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
        val sqlQuery = createIlikeSqlQuery(keyword)
        return queryDbForSpxList(sqlQuery)
    }

    private fun createIlikeSqlQuery(keyword: String): String {
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

    fun updateSpxFieldQuery(variableId: Int, query: String): Boolean {
        val sqlQuery = createSqlQueryToUpdateSpxFieldQueryByVariableId(variableId, query)
        return try {
            jdbcTemplate.update(sqlQuery)
            sqlCounter.labels("advertiser_smart_px_variables", "update", "ok").inc()
            true
        } catch (e: Exception) {
            log.error("unknown db exception to update data. sql=[$sqlQuery]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "update", "error").inc()
            false
        }
    }

    private fun createSqlQueryToUpdateSpxFieldQueryByVariableId(variableId: Int, query: String): String {
        return """
           UPDATE advertiser_smart_px_variables
           SET query = '$query'
           WHERE variable_id = '$variableId'
        """.trimIndent()
    }

    /**
     * Returns the status of pixels update.
     *
     * @return true for success
     *         null for unknown db error
     *         false for unknown db exception
     */
    fun batchInsertSPXsBySqlQuery(list: List<AdvertiserSmartPxVariables>, sql: String): Boolean? {
        val listSize = list.size
        val logString = getSpxListFieldQueryInfoString(list)
        return try {
            val resultSize = batchUpdateSpxAdvertiserIdAndQueryBySqlQuery(list, sql).size
            if (resultSize == listSize) {
                log.debug("$resultSize spx have been created")
                sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "ok").inc()
                true
            } else {
                log.error("problems with db batch update: recovery needed. returned result=[$resultSize]; $logString")
                sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "error").inc()
                null
            }
        } catch (e: Exception) {
            log.error("unknown db exception to batch update. error message=[${e.message}]; $logString")
            sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "error").inc()
            false
        }
    }

    fun batchUpdateSpxAdvertiserIdAndQueryBySqlQuery(list: List<AdvertiserSmartPxVariables>, sql: String): IntArray {
        return jdbcTemplate.batchUpdate(
            sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                    val spx = list[i]
                    ps.setInt(1, spx.advertiserId)
                    ps.setString(2, spx.query)
                }
                override fun getBatchSize(): Int {
                    return list.size
                }
            }
        )
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
