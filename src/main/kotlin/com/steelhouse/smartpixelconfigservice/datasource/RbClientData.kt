package com.steelhouse.smartpixelconfigservice.datasource

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.dao.Spx
import com.steelhouse.smartpixelconfigservice.datasource.repository.RbIntegrationRepository
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.createFieldQueryOfSpxForRbClientAdvId
import com.steelhouse.smartpixelconfigservice.util.getSpxListFieldQueryInfoString
import com.steelhouse.smartpixelconfigservice.util.keywordToFindSpxForRbClientAdvId
import com.steelhouse.smartpixelconfigservice.util.keywordToFindSpxForRbClientUid
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RbClientData(
    private val sqlCounter: Counter,
    private val spx: Spx,
    private val spxRepository: SpxRepository,
    private val rbIntegrationRepository: RbIntegrationRepository,
    private val multipleTablesData: MultipleTablesData
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun deleteRbClient(advertiserId: Int, variableIds: List<Int>): Boolean {
        return try {
            spxRepository.deleteAllById(variableIds)
            rbIntegrationRepository.deleteById(advertiserId)
            true
        } catch (e: Exception) {
            // Rollback transaction if any update fails
            log.error("unknown db exception to batch update multiple tables. error message=[${e.message}]")
            sqlCounter.labels("multiple_tables", "batch_update", "error").inc()
            false
        }
    }

    fun updateRbClientRbAdvId(advertiserId: Int, newRbAdvId: String, variableId: Int): Boolean {
        val queryList = createQueryListToUpdateRbAdvId(advertiserId, newRbAdvId, variableId)
        val updateStatus = multipleTablesData.updateMultipleTables(queryList)
        log.debug("${updateStatus.numOfRowsMatched} rows updated") // expect to be 2
        if (updateStatus.numOfRowsMatched != 2) {
            log.error("wrong rb client found in db: rockerbox_integration and spx do not match. advertiserId=[$advertiserId]")
        }
        return updateStatus.isExecuted
    }

    /**
     * Creates queries to update Rockerbox client's rockerbox advertiser id (rb_adv_id).
     */
    private fun createQueryListToUpdateRbAdvId(advertiserId: Int, newRbAdvId: String, variableId: Int): List<String> {
        val spxFieldQuery = newRbAdvId.createFieldQueryOfSpxForRbClientAdvId()
        val queryToUpdateSpx = """
           UPDATE advertiser_smart_px_variables
           SET query = '$spxFieldQuery'
           WHERE variable_id = '$variableId'
        """.trimIndent()
        val queryToUpdateRbIntegration = """
           UPDATE rockerbox_integration
           SET rb_adv_id = '$newRbAdvId', update_time = current_timestamp
           WHERE advertiser_id = '$advertiserId';
        """.trimIndent()
        return listOf(queryToUpdateSpx, queryToUpdateRbIntegration)
    }

    /**
     * Gets all the Rockerbox client's getRockerBoxUID(uid) spx from advertiser_smart_px_variables table.
     * Note: This scans all the data in the table by field query. Use it carefully.
     */
    fun getRbClientsUidSpxList(): List<AdvertiserSmartPxVariables>? {
        return spx.getSpxListByFieldQueryKeyword(keywordToFindSpxForRbClientUid)
    }

    /**
     * Gets all the Rockerbox clients' getRockerBoxAdvID(advId) spx from advertiser_smart_px_variables table.
     * Note: This scans all the data in the table by field query. Use it carefully.
     */
    fun getRbClientsAdvIdSpxList(): List<AdvertiserSmartPxVariables>? {
        return spx.getSpxListByFieldQueryKeyword(keywordToFindSpxForRbClientAdvId)
    }

    /**
     * Returns the list of Rockerbox client advertiser_smart_px_variables rows created.
     *
     * @return empty list for unknown db exception. No recovery is needed.
     *         list of rows for successful db transaction.
     *         null for db error. Further recovery is needed.
     */
    fun insertRbClientSpxListAndReturnRows(list: List<AdvertiserSmartPxVariables>): List<Map<String?, Any?>>? {
        // SQL query to insert a spx record with the advertiser_id and query as variables
        // trpx_call_parameter_defaults_id is always '34'
        // query_type is always '3'
        // active is always 'true'
        // endpoint is always 'spx'
        val sqlToInsertSpxAdvertiserIdAndQuery = """
            INSERT INTO advertiser_smart_px_variables
            (advertiser_id, trpx_call_parameter_defaults_id, query, query_type, active, regex, regex_replace, regex_replace_value, regex_replace_modifier, endpoint)
            VALUES(?, 34, ?, 3, true, null, null, null, null, 'spx');
        """.trimIndent()
        val listSize = list.size
        val logString = getSpxListFieldQueryInfoString(list)
        val rows: List<Map<String?, Any?>>
        try {
            rows = spx.batchUpdateBySqlQueryAndReturnRows(list, sqlToInsertSpxAdvertiserIdAndQuery)
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
}
