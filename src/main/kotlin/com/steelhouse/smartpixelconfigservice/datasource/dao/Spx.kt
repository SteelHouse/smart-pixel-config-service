package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.getSpxListInfoString
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
        val query = """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE advertiser_id = '$advertiserId';
        """
        return queryDbForSpxList(query)
    }

    @Cacheable("spxListByFieldQueryCache")
    fun getSpxListByFieldQueryKeyword(keyword: String): List<AdvertiserSmartPxVariables>? {
        val sql = createIlikeSqlQuery(keyword)
        return queryDbForSpxList(sql)
    }

    private fun createIlikeSqlQuery(keyword: String): String {
        return """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE query ILIKE '%$keyword%';
        """
    }

    private fun queryDbForSpxList(sql: String): List<AdvertiserSmartPxVariables>? {
        try {
            return jdbcTemplate.query(sql, BeanPropertyRowMapper(AdvertiserSmartPxVariables::class.java))
        } catch (e: EmptyResultDataAccessException) {
            log.debug("no spx found in db. sql=[$sql]")
            return emptyList()
        } catch (e: Exception) {
            log.error("unknown db exception to get spx. sql=[$sql]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "select", "error").inc()
        }
        return null
    }

    fun updateSpxFieldQuery(variableId: Int, query: String): Boolean {
        val sql = createSqlQueryToUpdateSpxFieldQuery(variableId, query)
        return try {
            jdbcTemplate.update(sql)
            sqlCounter.labels("advertiser_smart_px_variables", "update", "ok").inc()
            true
        } catch (e: Exception) {
            log.error("unknown db exception to update spx. sql=[$sql]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "update", "error").inc()
            false
        }
    }

    private fun createSqlQueryToUpdateSpxFieldQuery(variableId: Int, query: String): String {
        return """
           UPDATE advertiser_smart_px_variables
           SET query = '$query'
           WHERE variable_id = '$variableId'
        """
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
        val pixelsInfoString = getSpxListInfoString(list)
        return try {
            val resultSize = batchUpdateSpxAdvertiserIdAndQueryBySqlQuery(list, sql).size
            if (resultSize == listSize) {
                log.debug("$resultSize pixels have been created")
                sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "ok").inc()
                true
            } else {
                log.error("problems with db batch update: recovery needed. returned result=[$resultSize]; $pixelsInfoString")
                sqlCounter.labels("advertiser_smart_px_variables", "batch_update", "error").inc()
                null
            }
        } catch (e: Exception) {
            log.error("unknown db exception to batch update. error message=[${e.message}]; $pixelsInfoString")
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
            log.error("unknown db exception to delete spx. variableIds=[$ids]; error message=[${e.message}]")
            sqlCounter.labels("advertiser_smart_px_variables", "delete", "error").inc()
            false
        }
    }
}
