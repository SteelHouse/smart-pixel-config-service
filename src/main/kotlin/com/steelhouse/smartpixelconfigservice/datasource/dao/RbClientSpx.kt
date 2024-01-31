package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.createRbClientAdvIdSpxFieldQuery
import com.steelhouse.smartpixelconfigservice.util.createRbClientUidSpxFieldQuery
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

const val rbClientUidSpxTag = "rbClientUidSpx"
const val rbClientAdvIdSpxTag = "rbClientAdvIdSpx"

@Component
class RbClientSpx(
    sqlCounter: Counter,
    jdbcTemplate: JdbcTemplate,
    spxRepository: SpxRepository
) : Spx(sqlCounter, jdbcTemplate, spxRepository) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    // SQL query to insert a spx record with the advertiser_id and query as variables
    val sqlToInsertSpxAdvertiserIdAndQuery = """
            INSERT INTO advertiser_smart_px_variables
            (advertiser_id, trpx_call_parameter_defaults_id, query, query_type, active, regex, regex_replace, regex_replace_value, regex_replace_modifier, endpoint)
            VALUES(?, 34, ?, 3, true, null, null, null, null, 'spx');
    """.trimIndent()

    // Rockerbox client's getRockerBoxUID(uid) spx looks like:
    // let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=${document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();
    val rbClientUidSpxFieldQueryKeyword = "getRockerBoxUID()"

    // Rockerbox client's getRockerBoxAdvID(advId) spx looks like:
    // let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=test_id"; }; getRockerBoxAdvID();
    val rbClientAdvIdSpxFieldQueryKeyword = "getRockerBoxAdvID()"

    @Cacheable("rbClientsUidSpxListCache")
    fun getRbClientsUidSpxList(): List<AdvertiserSmartPxVariables>? {
        return getSpxListByFieldQueryKeyword(rbClientUidSpxFieldQueryKeyword)
    }

    @Cacheable("rbClientsAdvIdSpxListCache")
    fun getRbClientsAdvIdSpxList(): List<AdvertiserSmartPxVariables>? {
        return getSpxListByFieldQueryKeyword(rbClientAdvIdSpxFieldQueryKeyword)
    }

    fun isRbClientSpx(query: String): Boolean {
        return isRbClientUidSpx(query) || isRbClientAdvIdSpx(query)
    }

    fun isRbClientUidSpx(query: String): Boolean {
        return query.contains(rbClientUidSpxFieldQueryKeyword)
    }

    fun isRbClientAdvIdSpx(query: String): Boolean {
        return query.contains(rbClientAdvIdSpxFieldQueryKeyword)
    }

    fun updateRbClientAdvIdSpx(variableId: Int, rbAdvId: String): Boolean {
        val newQuery = createRbClientAdvIdSpxFieldQuery(rbAdvId)
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
        return batchInsertSPXsBySqlQuery(list, sqlToInsertSpxAdvertiserIdAndQuery)
    }

    fun createRbClientAdvIdSpx(aid: Int, rbAdvId: String): AdvertiserSmartPxVariables {
        val spx = AdvertiserSmartPxVariables()
        spx.advertiserId = aid
        spx.query = createRbClientAdvIdSpxFieldQuery(rbAdvId)
        return spx
    }

    fun createRbClientUid(aid: Int): AdvertiserSmartPxVariables {
        val spx = AdvertiserSmartPxVariables()
        spx.advertiserId = aid
        spx.query = createRbClientUidSpxFieldQuery()
        return spx
    }
}
