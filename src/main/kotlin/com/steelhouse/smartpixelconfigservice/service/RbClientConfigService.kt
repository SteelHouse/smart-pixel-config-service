package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.config.RbClientConfig
import com.steelhouse.smartpixelconfigservice.datasource.RbClientData
import com.steelhouse.smartpixelconfigservice.datasource.dao.RbIntegration
import com.steelhouse.smartpixelconfigservice.datasource.dao.SpxForRbClient
import com.steelhouse.smartpixelconfigservice.datasource.dao.rbClientAdvIdSpxTag
import com.steelhouse.smartpixelconfigservice.datasource.dao.rbClientUidSpxTag
import com.steelhouse.smartpixelconfigservice.util.createSpxForRbClientAdvId
import com.steelhouse.smartpixelconfigservice.util.createSpxForRbClientUid
import com.steelhouse.smartpixelconfigservice.util.findRbAdvIdInString
import com.steelhouse.smartpixelconfigservice.util.getSpxFieldQueryInfoString
import com.steelhouse.smartpixelconfigservice.util.getSpxListFieldQueryInfoString
import com.steelhouse.smartpixelconfigservice.util.isSpxForRbClient
import com.steelhouse.smartpixelconfigservice.util.isSpxForRbClientAdvId
import com.steelhouse.smartpixelconfigservice.util.isSpxForRbClientUid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RbClientConfigService(
    private val spx: SpxForRbClient,
    private val rbIntegration: RbIntegration,
    private val rbClientData: RbClientData
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Returns a list of all the Rockerbox client's mntn advertiserId or null if there are errors.
     * The list is retrieved from the getRockerBoxUID spx list from advertiser_smart_px_variables table.
     */
    fun getRbClientsAdvertiserIdListFromUidSpx(): MutableList<Int>? {
        val rbClientsUidSpxList = spx.getRbClientsUidSpxList() ?: return null
        val list = mutableListOf<Int>()
        rbClientsUidSpxList.forEach { spx -> list.add(spx.advertiserId) }
        return list
    }

    /**
     * Returns a map of all the Rockerbox client's mntn advertiserId -> rbAdvertiserId or null if there are errors.
     * It is guaranteed that the advertiser in the map are valid as having two Rockerbox related smart pixels
     * from advertiser_smart_px_variables table.
     */
    fun getRbClients(): Map<Int, String>? {
        val rbClientAdvIdSpxList = spx.getRbClientsAdvIdSpxList() ?: return null
        val rbClientAdvertiserIdListFromUidSpx = getRbClientsAdvertiserIdListFromUidSpx() ?: return null
        return getValidRbClientInfoMap(rbClientAdvIdSpxList, rbClientAdvertiserIdListFromUidSpx)
    }

    /**
     * Matches the getRockerBoxAdvID spx and getRockerBoxUID spx by advertiserId, filters out and log the solo ones
     * and returns a map of advertiserId --> rbAdvertiserId.
     */
    fun getValidRbClientInfoMap(advIdSpxList: List<AdvertiserSmartPxVariables>, aidListFromUidSpx: MutableList<Int>): Map<Int, String> {
        val map = mutableMapOf<Int, String>()
        advIdSpxList.forEach { spx ->
            val aid = spx.advertiserId
            if (aidListFromUidSpx.contains(aid)) {
                val rbAdvId = retrieveRbAdvIdFromSpxFieldQuery(spx)
                if (rbAdvId != null) map[spx.advertiserId] = rbAdvId
                aidListFromUidSpx.remove(aid)
            } else {
                log.error("wrong rb client found in db: advertiser has getRockerBoxAdvID spx but no getRockerBoxUID spx. advertiserId=[$aid]")
            }
        }
        if (aidListFromUidSpx.isNotEmpty()) {
            aidListFromUidSpx.forEach {
                log.error("wrong rb client found in db: advertiser has getRockerBoxUID spx but no getRockerBoxAdvID spx. advertiserId=[$it]")
            }
        }
        return map
    }

    /**
     * Returns a map of Rockerbox client related smart pixels by mntn advertiserId or null if there are errors.
     * Steps:
     * - retrieve the data from rockerbox_integration table using advertiserId and get the variable ids for SPXs
     * - retrieve the data from advertiser_smart_px_variables table using the variable ids
     * - generate a map which is guaranteed to be in the following format and have two key-value pairs
     * {
     *      "rbClientAdvIdSpx" --> <getRockerBoxAdvID spx>,
     *      "rbClientUidSpx"   --> <getRockerBoxUID spx>
     * }
     */
    fun getRbClientSpxMapByAdvertiserId(advertiserId: Int): Map<String, AdvertiserSmartPxVariables>? {
        // get the data from rockerbox_integration table
        val rbClientList = rbIntegration.getRbIntegrationListByAdvertiserId(advertiserId) ?: return null
        if (rbClientList.isEmpty()) return emptyMap()
        if (rbClientList.size != 1) {
            log.error("wrong rb client found in db: more than 1 rb_integration found for advertiserId=[$advertiserId]")
            return null
        }
        val rbClient = rbClientList[0]

        // get the data from advertiser_smart_px_variables table
        val spxList = spx.getSpxListByVariableIds(listOf(rbClient.rbAdvIdMappingVariableId, rbClient.rbUidMappingVariableId))
        if (spxList == null || spxList.size != 2) {
            log.error("wrong rb client found in db: no spx or not exactly 2 spx found for advertiserId=[$advertiserId]")
            return null
        }

        // validate and return the spx map
        val map = matchSpxWithRbClientTag(spxList)
        return if (doesRbClientHaveValidSPXs(map)) map else null
    }

    /**
     * Returns a map of Rockerbox client related smart pixels by mntn advertiserId or null if there are errors.
     * The map is guaranteed to be in the following format and have two key-value pairs.
     * {
     *      "rbClientAdvIdSpx" --> <getRockerBoxAdvID spx>,
     *      "rbClientUidSpx"   --> <getRockerBoxUID spx>
     * }
     * The map is retrieved from advertiser_smart_px_variables table.
     */
    fun getRbClientSpxMapFromSpxTableByAdvertiserId(advertiserId: Int): Map<String, AdvertiserSmartPxVariables>? {
        val map = getRbClientSpxInfoFromSpxTableByAdvertiserId(advertiserId) ?: return null
        return if (doesRbClientHaveValidSPXs(map)) map else null
    }

    /**
     * Returns a map of Rockerbox client related smart pixels by mntn advertiserId or null if there are errors.
     * The map is in the following format but there is NO guarantee that the map contains both Rockerbox client smart pixels.
     * {
     *      <rb_spx_type> --> <spx>
     * }
     * The map is retrieved from advertiser_smart_px_variables table.
     */
    fun getRbClientSpxInfoFromSpxTableByAdvertiserId(advertiserId: Int): Map<String, AdvertiserSmartPxVariables>? {
        val spxList = spx.getSpxListByAdvertiserId(advertiserId) ?: return null
        return matchSpxWithRbClientTag(spxList)
    }

    /**
     * Returns a map of Rockerbox client related spx-tag and corresponding spx.
     */
    fun matchSpxWithRbClientTag(spxList: List<AdvertiserSmartPxVariables>): Map<String, AdvertiserSmartPxVariables> {
        if (spxList.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, AdvertiserSmartPxVariables>()
        spxList.forEach { spx ->
            val query = spx.query
            if (query.isSpxForRbClientUid()) map[rbClientUidSpxTag] = spx
            if (query.isSpxForRbClientAdvId()) map[rbClientAdvIdSpxTag] = spx
        }
        return map
    }

    /**
     * Determines whether a Rockerbox client has valid SPXs by checking:
     * - it has 2 smart pixels, AND
     * - 2 smart pixels are getRockerBoxAdvID spx and getRockerBoxUID spx.
     */
    private fun doesRbClientHaveValidSPXs(map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        if (map.size == 2 && map.containsKey(rbClientUidSpxTag) && map.containsKey(rbClientAdvIdSpxTag)) return true
        val logString = getSpxListFieldQueryInfoString(map.values.toList())
        log.error("wrong rb client found in db: missing getRockerBoxAdvID or getRockerBoxUID spx. $logString")
        return false
    }

    /**
     * Creates a new Rockerbox client, and returns a boolean flag indicating the status of action.
     * If there are errors during the creation process, remove the problematic spx if possible and return false.
     */
    fun insertRbClient(config: RbClientConfig): Boolean {
        log.debug("need to create a new rb client")

        // Double-check whether the rbAdvId is unique
        val advertiserId = config.advertiserId
        val rbAdvId = config.rbAdvId
        if (!isRbAdvIdUnique(rbAdvId)) return false

        // Insert 2 rows into advertiser_smart_px_variables table
        val rbClientAdvIdSpx = createSpxForRbClientAdvId(advertiserId, rbAdvId)
        val rbClientUidSpx = advertiserId.createSpxForRbClientUid()
        val insertedSpxRows = spx.insertRbClientSpxListAndReturnRows(listOf(rbClientAdvIdSpx, rbClientUidSpx))
        if (insertedSpxRows == null) {
            removeProblematicRbClientSPXsByAdvertiserId(advertiserId)
            return false
        }

        // Insert 1 row into rockerbox_integration table
        return if (insertedSpxRows.isEmpty()) {
            false
        } else {
            val rbAdvIdSpxVariableId = insertedSpxRows[0]["variable_id"] as? Int
            val rbUidSpxVariableId = insertedSpxRows[1]["variable_id"] as? Int
            insertRbIntegration(advertiserId, rbAdvId, rbAdvIdSpxVariableId, rbUidSpxVariableId)
        }
    }

    fun insertRbIntegration(advertiserId: Int, rbAdvId: String, rbAdvIdSpxVariableId: Int?, rbUidSpxVariableId: Int?): Boolean {
        if (rbAdvIdSpxVariableId == null || rbUidSpxVariableId == null) {
            removeProblematicRbClientSPXsByAdvertiserId(advertiserId)
            return false
        }
        val insertRbIntegrationSucceed = rbIntegration.insertRbIntegration(advertiserId, rbAdvId, rbAdvIdSpxVariableId, rbUidSpxVariableId)
        return if (insertRbIntegrationSucceed) {
            true
        } else {
            spx.deleteSPXsByVariableIds(listOf(rbAdvIdSpxVariableId, rbUidSpxVariableId))
            false
        }
    }

    /**
     * Returns whether the Rockerbox Advertiser ID is unique in advertiser_smart_px_variables table.
     * This is a safe net because the client should have done the uniqueness validation on their end.
     * However, if the input value is ever not unique, we will mess up the data and there is no way to recover.
     */
    fun isRbAdvIdUnique(inputRbAdvId: String): Boolean {
        val existingRbAdvIds = rbIntegration.getAllRbAdvIds() ?: return false
        val isIdUnique = !existingRbAdvIds.contains(inputRbAdvId)
        if (!isIdUnique) {
            log.error("client request error: client should have done rbAdvId=[$inputRbAdvId] uniqueness validation")
        }
        return isIdUnique
    }

    /**
     * Returns whether the Rockerbox Advertiser ID is unique in advertiser_smart_px_variables table.
     * This is a safe net because the client should have done the uniqueness validation on their end.
     * However, if the input value is ever not unique, we will mess up the data and there is no way to recover.
     */
    fun isRbAdvIdUniqueInSpxTable(config: RbClientConfig): Boolean {
        val inputAdvertiserId = config.advertiserId
        val inputRbAdvId = config.rbAdvId
        val advIdSpxListFromSpxTable = spx.getRbClientsAdvIdSpxList() ?: return false
        if (advIdSpxListFromSpxTable.isEmpty()) return true
        advIdSpxListFromSpxTable.forEach { spx ->
            val dbRbAdvId = retrieveRbAdvIdFromSpxFieldQuery(spx)
            if (dbRbAdvId != null) {
                if (dbRbAdvId == inputRbAdvId) {
                    val dbAdverserId = spx.advertiserId
                    if (dbAdverserId == inputAdvertiserId) {
                        log.debug("same rdAdvId is allowed for the same advertiser")
                    } else {
                        log.error(
                            "client request error: no uniqueness validation on rbAdvId. input advertiserId=[$inputAdvertiserId]; input rbAdvId=[$inputRbAdvId]; " +
                                "\ndb advertiserId=[$dbAdverserId]; db rbAdvId=[$dbRbAdvId]; db spx=${getSpxFieldQueryInfoString(spx)}"
                        )
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun removeProblematicRbClientSPXsByAdvertiserId(advertiserId: Int) {
        val map = getRbClientSpxInfoFromSpxTableByAdvertiserId(advertiserId)
        if (map.isNullOrEmpty()) return
        deleteRbClientSPXs(advertiserId, map)
    }

    /**
     * Updates the Rockerbox client's getRockerBoxAdvID spx with new Rockerbox advertiserId if applicable,
     * and returns a boolean flag indicating the status of action.
     */
    fun updateRbClient(newConfig: RbClientConfig, map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        val advertiserId = newConfig.advertiserId

        // Validate the Rockerbox client's getRockerBoxAdvID spx
        val rbClientAdvIdSpx = map[rbClientAdvIdSpxTag]
        if (rbClientAdvIdSpx == null || rbClientAdvIdSpx.variableId == null) {
            log.error("wrong rb client found in db: getRockerBoxAdvID spx is not valid. advertiserId=[$advertiserId]")
            return false
        }
        val variableId = rbClientAdvIdSpx.variableId
        val oldRbAdvId = retrieveRbAdvIdFromSpxFieldQuery(rbClientAdvIdSpx) ?: return false

        // Validate the new rb_adv_id is different from the old one
        val newRbAdvId = newConfig.rbAdvId
        if (newRbAdvId == oldRbAdvId) {
            log.debug("no change to the rockerbox advertiser id. no action needed.")
            return true
        }

        // Double-check whether the rbAdvId is unique
        if (!isRbAdvIdUnique(newRbAdvId)) return false

        // Update the Rockerbox client
        return rbClientData.updateRbClientRbAdvId(advertiserId, newRbAdvId, variableId)
    }

    /**
     * Deletes a Rockerbox client's advertiser_smart_px_variables by mntn advertiserId if applicable,
     * and returns a boolean flag indicating the status of action.
     */
    fun deleteRbClientSPXs(advertiserId: Int, map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        val spxList = map.values.toList()
        val variableIds = mutableListOf<Int>()

        // Double-check the pixels are rb client spx before deletion and add the pixel's variableId to the deletion list if applicable
        // In theory, there should be no error because the input map is retrieved by this service and guaranteed to be valid.
        spxList.forEach { spx ->
            val query = spx.query
            val logString = getSpxFieldQueryInfoString(spx)
            if (spx.advertiserId != advertiserId) {
                // In theory, this error should never happen because the map is retrieved by this service and guaranteed to be valid.
                log.error("wrong rb client found in db: wrong spx retrieved for advertiserId=[$advertiserId]. spx=$logString")
                return false
            }
            if (query.isSpxForRbClient()) {
                log.debug("added rb client spx to deletion list for advertiserID=[$advertiserId]... spx=$logString")
                variableIds.add(spx.variableId)
            } else {
                // In theory, this error should never happen because the map is retrieved by this service and guaranteed to be valid.
                log.error("wrong rb client found in db: non rb client spx retrieved. advertiserID=[$advertiserId]. spx=$logString")
                return false
            }
        }
        return spx.deleteSPXsByVariableIds(variableIds)
    }

    /**
     * Deletes a Rockerbox client and returns a boolean flag indicating the status of action.
     */
    fun deleteRbClient(advertiserId: Int, map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        val spxList = map.values.toList()
        val variableIds = spxList.map { it.variableId }
        return rbClientData.deleteRbClient(advertiserId, variableIds)
    }

    private fun retrieveRbAdvIdFromSpxFieldQuery(spx: AdvertiserSmartPxVariables?): String? {
        if (spx == null || spx.query == null) return null
        val result = findRbAdvIdInString(spx.query)
        if (result == null) {
            log.error("wrong rb client found in db: no rb_adv_id in getRockerBoxAdvID spx. spx=${getSpxFieldQueryInfoString(spx)}")
            return null
        }
        return result
    }
}
