package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.createRbClientAdvIdSpxQuery
import com.steelhouse.smartpixelconfigservice.util.createRbClientUidSpxQuery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

const val rbUidSpxTag = "rbUidSpx"
const val rbAdvIdSpxTag = "rbAdvIdSpx"

@Component
class RbClientSpxDaoImpl(
    private val jdbcTemplate: JdbcTemplate,
    spxRepository: SpxRepository
) : SpxDaoImpl(jdbcTemplate, spxRepository) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    // Rockerbox client's uid spx looks like:
    // let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=${document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();
    val rbClientUidSpxKeyword = "getRockerBoxUID()"

    // Rockerbox client's advId spx looks like:
    // let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=test_id"; }; getRockerBoxAdvID();
    val rbClientAdvIdSpxKeyword = "getRockerBoxAdvID()"

    @Cacheable("rbClientsUidSPXsCache")
    fun getRbClientsUidSPXs(): List<AdvertiserSmartPxVariables>? {
        return getAdvertiserSmartPxVariablesByQueryKeyword(rbClientUidSpxKeyword)
    }

    @Cacheable("rbClientsAdvIdSPXsCache")
    fun getRbClientsAdvIdSPXs(): List<AdvertiserSmartPxVariables>? {
        return getAdvertiserSmartPxVariablesByQueryKeyword(rbClientAdvIdSpxKeyword)
    }

    fun getSPXsByAdvertiserId(advertiserId: Int): List<AdvertiserSmartPxVariables>? {
        return getAdvertiserSmartPxVariablesByAdvertiserId(advertiserId)
    }

    fun deleteSPXsByVariableIds(advertiserId: Int, variableIds: List<Int>): Boolean {
        val ids = variableIds.joinToString()
        return try {
            deleteSPXsByIds(variableIds)
            log.debug("spx deletion succeed. advertiserId=[$advertiserId]; variableIds=[$ids]")
            true
        } catch (e: Exception) {
            log.error("unknown db exception to delete spx. advertiserId=[$advertiserId]; variableIds=[$ids]. error=[${e.message}]")
            false
        }
    }

    fun isRbClientSpxByQuery(query: String): Boolean {
        return isRbClientUidSpxByQuery(query) || isRbClientAdvIdSpxByQuery(query)
    }

    fun isRbClientUidSpxByQuery(query: String): Boolean {
        return query.contains(rbClientUidSpxKeyword)
    }

    fun isRbClientAdvIdSpxByQuery(query: String): Boolean {
        return query.contains(rbClientAdvIdSpxKeyword)
    }

    fun updateRbClientAdvIdSpx(variableId: Int, rbAdvId: String): Boolean {
        val newQuery = createRbClientAdvIdSpxQuery(rbAdvId)
        log.debug("need to update an existing rb client's AdvId spx. variableId=[$variableId]; new query=[$newQuery]")
        return updateAdvertiserSmartPxVariablesQuery(variableId, newQuery)
    }

    /**
     * Returns the status of Rockerbox client pixels creation.
     *
     * @return true for success
     *         null for unknown db error, need further action to recover
     *         false for unknown db exception
     */
    fun createRbClientSPXs(advertiserId: Int, rbAdvId: String): Boolean? {
        val rbClientAdvIdSpx = createRbClientAdvIdSpx(advertiserId, rbAdvId)
        log.debug("created rb client getRockerBoxAdvID spx. query=$rbClientAdvIdSpx.query")
        val rbClientUidSpx = createRbClientUid(advertiserId)
        log.debug("created rb client getRockerBoxUID spx. query=$rbClientUidSpx.query")

        return try {
            val resultSize = insertRbClientSPXs(listOf(rbClientAdvIdSpx, rbClientUidSpx)).size
            if (resultSize == 2) {
                log.debug("2 pixels have been created")
                true
            } else {
                log.error("problems with db spx creation: returned result=[$resultSize]. recovery needed. advertiserId=[$advertiserId]; rbAdvId=[$rbAdvId]")
                null
            }
        } catch (e: Exception) {
            log.error("unknown db exception to create SPXs. advertiserId=[$advertiserId]. error=[${e.message}]")
            false
        }
    }

    fun insertRbClientSPXs(spxList: List<AdvertiserSmartPxVariables>): IntArray {
        val sql = """
            INSERT INTO advertiser_smart_px_variables
            (advertiser_id, trpx_call_parameter_defaults_id, query, query_type, active, regex, regex_replace, regex_replace_value, regex_replace_modifier, endpoint)
            VALUES(?, 34, ?, 3, true, null, null, null, null, 'spx');
        """.trimIndent()
        return jdbcTemplate.batchUpdate(
            sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: java.sql.PreparedStatement, i: Int) {
                    val spx = spxList[i]
                    ps.setInt(1, spx.advertiserId)
                    ps.setString(2, spx.query)
                }
                override fun getBatchSize(): Int {
                    return spxList.size
                }
            }
        )
    }

    fun createRbClientAdvIdSpx(aid: Int, rbAdvId: String): AdvertiserSmartPxVariables {
        val spx = AdvertiserSmartPxVariables()
        spx.advertiserId = aid
        spx.query = createRbClientAdvIdSpxQuery(rbAdvId)
        return spx
    }

    fun createRbClientUid(aid: Int): AdvertiserSmartPxVariables {
        val spx = AdvertiserSmartPxVariables()
        spx.advertiserId = aid
        spx.query = createRbClientUidSpxQuery()
        return spx
    }
}
