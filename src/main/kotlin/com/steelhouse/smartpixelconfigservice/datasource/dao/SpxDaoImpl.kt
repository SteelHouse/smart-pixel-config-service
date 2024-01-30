package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class SpxDaoImpl(
    private val jdbcTemplate: JdbcTemplate,
    spxRepository: SpxRepository
) : SpxDao(spxRepository) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)
    fun getAdvertiserSmartPxVariablesByAdvertiserId(advertiserId: Int): List<AdvertiserSmartPxVariables>? {
        val query = """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE advertiser_id = '$advertiserId';
        """
        return queryDbForSPXs(query)
    }

    fun getAdvertiserSmartPxVariablesByQueryKeyword(keyword: String): List<AdvertiserSmartPxVariables>? {
        val query = createIlikeQuery(keyword)
        return queryDbForSPXs(query)
    }

    private fun createIlikeQuery(keyword: String): String {
        return """
            SELECT * 
            FROM advertiser_smart_px_variables
            WHERE query ILIKE '%$keyword%';
        """
    }

    private fun queryDbForSPXs(query: String): List<AdvertiserSmartPxVariables>? {
        try {
            return jdbcTemplate.query(query, BeanPropertyRowMapper(AdvertiserSmartPxVariables::class.java))
        } catch (e: EmptyResultDataAccessException) {
            log.debug("no spx found in db. query=[$query]")
            return emptyList()
        } catch (e: Exception) {
            log.error("unknown db exception to get spx. query=[$query]; error message=[${e.message}]")
        }
        return null
    }

    fun updateAdvertiserSmartPxVariablesQuery(variableId: Int, query: String): Boolean {
        val sqlQuery = createSqlQueryToUpdateFieldQuery(variableId, query)
        return try {
            jdbcTemplate.update(sqlQuery)
            true
        } catch (e: Exception) {
            log.error("unknown db exception to update spx. query=[$sqlQuery]; error message=[${e.message}]")
            false
        }
    }

    private fun createSqlQueryToUpdateFieldQuery(variableId: Int, query: String): String {
        return """
           UPDATE advertiser_smart_px_variables
           SET query = '$query'
           WHERE variable_id = '$variableId'
        """
    }
}
