package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.createFieldQueryOfSpxForRbClientAdvId
import com.steelhouse.smartpixelconfigservice.util.keywordToFindSpxForRbClientAdvId
import com.steelhouse.smartpixelconfigservice.util.keywordToFindSpxForRbClientUid
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

const val rbClientUidSpxTag = "rbClientUidSpx"
const val rbClientAdvIdSpxTag = "rbClientAdvIdSpx"

/**
 * This class is for the Rockerbox integrated advertiser (RbClient) advertiser_smart_px_variables only.
 *
 * Each RbClient should have two specific pixels.
 * - getRockerBoxAdvID(advId): defines the Rockerbox advertiser id
 * - getRockerBoxUID(uid): retrieves the Rockerbox user id
 */
@Component
class SpxForRbClient(
    sqlCounter: Counter,
    jdbcTemplate: JdbcTemplate,
    spxRepository: SpxRepository
) : Spx(sqlCounter, jdbcTemplate, spxRepository) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Gets all the Rockerbox client's getRockerBoxUID(uid) spx.
     */
    fun getRbClientsUidSpxList(): List<AdvertiserSmartPxVariables>? {
        return getSpxListByFieldQueryKeyword(keywordToFindSpxForRbClientUid)
    }

    /**
     * Gets all the Rockerbox clients' getRockerBoxAdvID(advId) spx.
     */
    fun getRbClientsAdvIdSpxList(): List<AdvertiserSmartPxVariables>? {
        return getSpxListByFieldQueryKeyword(keywordToFindSpxForRbClientAdvId)
    }

    fun updateRbClientAdvIdSpx(variableId: Int, rbAdvId: String): Boolean {
        val newQuery = rbAdvId.createFieldQueryOfSpxForRbClientAdvId()
        log.debug("need to update an existing rb client's getRockerBoxAdvID spx. variableId=[$variableId]; new query=[$newQuery]")
        return updateSpxFieldQuery(variableId, newQuery)
    }

    /**
     * Returns the status of Rockerbox client pixels creation.
     *
     * @return true for success
     *         null for unknown db error, need further action to recover
     *         false for unknown db exception
     */
    fun insertRbClientSPXs(list: List<AdvertiserSmartPxVariables>): Boolean? {
        // SQL query to insert a spx record with the advertiser_id and query as variables
        val sqlToInsertSpxAdvertiserIdAndQuery = """
            INSERT INTO advertiser_smart_px_variables
            (advertiser_id, trpx_call_parameter_defaults_id, query, query_type, active, regex, regex_replace, regex_replace_value, regex_replace_modifier, endpoint)
            VALUES(?, 34, ?, 3, true, null, null, null, null, 'spx');
        """.trimIndent()
        return batchInsertSPXsBySqlQuery(list, sqlToInsertSpxAdvertiserIdAndQuery)
    }
}
