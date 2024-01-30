package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.getSpxListInfoString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class Spx(
    private val jdbcTemplate: JdbcTemplate,
    private val spxRepository: SpxRepository
) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    fun getSpxListByAdvertiserId(advertiserId: Int): List<AdvertiserSmartPxVariables>? {
        val query = """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE advertiser_id = '$advertiserId';
        """
        return queryDbForSpxList(query)
    }

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
        }
        return null
    }

    fun updateSpxFieldQuery(variableId: Int, query: String): Boolean {
        val sql = createSqlQueryToUpdateSpxFieldQuery(variableId, query)
        return try {
            jdbcTemplate.update(sql)
            true
        } catch (e: Exception) {
            log.error("unknown db exception to update spx. sql=[$sql]; error message=[${e.message}]")
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
    fun batchCreateSPXsBySqlQuery(list: List<AdvertiserSmartPxVariables>, sql: String): Boolean? {
        val listSize = list.size
        val pixelsInfoString = getSpxListInfoString(list)
        return try {
            val resultSize = batchUpdateSpxAdvertiserIdAndQueryBySqlQuery(list, sql).size
            if (resultSize == listSize) {
                log.debug("$resultSize pixels have been created")
                true
            } else {
                log.error("problems with db batch update: returned result=[$resultSize]. recovery needed. $pixelsInfoString")
                null
            }
        } catch (e: Exception) {
            log.error("unknown db exception in batch update. error=[${e.message}]; $pixelsInfoString")
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

    fun deleteSPXsByIds(variableIds: List<Int>) {
        return spxRepository.deleteAllById(variableIds)
    }
}
