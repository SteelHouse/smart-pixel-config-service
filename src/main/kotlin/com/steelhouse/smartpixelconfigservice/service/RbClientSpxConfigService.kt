package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.dao.RbClientSpxDaoImpl
import com.steelhouse.smartpixelconfigservice.datasource.dao.rbAdvIdSpxTag
import com.steelhouse.smartpixelconfigservice.datasource.dao.rbUidSpxTag
import com.steelhouse.smartpixelconfigservice.model.RbClientConfig
import com.steelhouse.smartpixelconfigservice.util.findRegexMatchResultInString
import com.steelhouse.smartpixelconfigservice.util.getSpxInfoString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RbClientSpxConfigService(
    private val rbClientSpxDao: RbClientSpxDaoImpl
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Returns a list of all the Rockerbox client's mntn advertiserId or null if there are errors.
     */
    fun getRbClientsAdvertiserIds(): List<Int>? {
        val rbClientsUidSPXs = rbClientSpxDao.getRbClientsUidSPXs() ?: return null
        val list = mutableListOf<Int>()
        rbClientsUidSPXs.forEach { spx -> list.add(spx.advertiserId) }
        return list
    }

    /**
     * Returns a map of all the Rockerbox client's mntn advertiserId -> rbAdvertiserId or null if there are errors.
     */
    fun getRbClients(): Map<Int, String>? {
        val rbClientsAdvIdSPXs = rbClientSpxDao.getRbClientsAdvIdSPXs() ?: return null
        val map = mutableMapOf<Int, String>()
        rbClientsAdvIdSPXs.forEach { spx ->
            val rbAdvId = retrieveRbAdvIdFromSpxQuery(spx)
            if (rbAdvId != null) map[spx.advertiserId] = rbAdvId
        }
        return map
    }

    /**
     * Returns a map of Rockerbox client related smart pixels by mntn advertiserId or null if there are errors.
     * The map is guaranteed to be in the following format and have two key-value pairs.
     * {
     *      "rbUidSpx"   --> <getRockerBoxAdvID spx>,
     *      "rbAdvIdSpx" --> <getRockerBoxUID spx>
     * }
     */
    fun getRbClientSpxMapByAdvertiserId(advertiserId: Int): Map<String, AdvertiserSmartPxVariables>? {
        val map = getRbClientSpxInfoFromDbByAdvertiserId(advertiserId) ?: return null
        if (map.isEmpty()) return emptyMap()
        return if (isRbClientValid(map)) map else null
    }

    /**
     * Returns a map of Rockerbox client related smart pixels by mntn advertiserId or null if there are errors.
     * The map is in the format of <rb_spx_type> --> <rb_spx>.
     * There is NO guarantee that the map contains both Rockerbox client smart pixels.
     */
    private fun getRbClientSpxInfoFromDbByAdvertiserId(advertiserId: Int): Map<String, AdvertiserSmartPxVariables>? {
        val pixels = rbClientSpxDao.getSPXsByAdvertiserId(advertiserId) ?: return null
        if (pixels.isEmpty()) return emptyMap()
        val map = mutableMapOf<String, AdvertiserSmartPxVariables>()
        pixels.forEach { spx ->
            val query = spx.query
            if (rbClientSpxDao.isRbClientUidSpxByQuery(query)) map[rbUidSpxTag] = spx
            if (rbClientSpxDao.isRbClientAdvIdSpxByQuery(query)) map[rbAdvIdSpxTag] = spx
        }
        return map
    }

    /**
     * Determines whether a Rockerbox client is valid by checking:
     * - it has 2 smart pixels, AND
     * - 2 smart pixels are getRockerBoxAdvID spx and getRockerBoxUID spx.
     */
    private fun isRbClientValid(map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        if (map.size == 2 && map.containsKey(rbUidSpxTag) && map.containsKey(rbAdvIdSpxTag)) return true
        var pixelsInfoString = ""
        map.values.forEach { pixel -> pixelsInfoString += getSpxInfoString(pixel) }
        log.error("wrong rb client found in db: missing getRockerBoxAdvID or getRockerBoxUID spx. $pixelsInfoString")
        return false
    }

    /**
     * Create SPXs for a new Rockerbox client, and returns a boolean flag indicating the status of action.
     * If there are errors during the spx creation process, remove the problematic spx if possible and return false.
     */
    fun createRbClient(rbClientConfig: RbClientConfig): Boolean {
        log.debug("need to create a new rb client")
        val creationSucceed = rbClientSpxDao.createRbClientSPXs(rbClientConfig.advertiserId, rbClientConfig.rbAdvId)
        if (creationSucceed == true || creationSucceed == false) return creationSucceed
        removeProblematicRbClientSPXs(rbClientConfig.advertiserId)
        return false
    }

    private fun removeProblematicRbClientSPXs(advertiserId: Int) {
        val map = getRbClientSpxInfoFromDbByAdvertiserId(advertiserId)
        if (map.isNullOrEmpty()) return
        deleteRbClient(advertiserId, map)
    }

    /**
     * Updates the Rockerbox client's getRockerBoxAdvID spx with new Rockerbox advertiserId if applicable,
     * and returns a boolean flag indicating the status of action.
     */
    fun updateRbClient(newConfig: RbClientConfig, map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        val advertiserId = newConfig.advertiserId

        // Validate the Rockerbox client's rbAdvId spx
        val rbAdvIdSpx = map[rbAdvIdSpxTag]
        if (rbAdvIdSpx == null || rbAdvIdSpx.variableId == null) {
            log.error("wrong rb client found in db: getRockerBoxAdvID spx is not valid. advertiserId=[$advertiserId]")
            return false
        }
        val variableId = rbAdvIdSpx.variableId
        val oldRbAdvId = retrieveRbAdvIdFromSpxQuery(rbAdvIdSpx) ?: return false

        // Validate the new rb_adv_id is different from the old one
        val newRbAdvId = newConfig.rbAdvId
        if (newRbAdvId == oldRbAdvId) {
            log.debug("no change to the rockerbox advertiser id. no action needed.")
            return true
        }

        // Update the Rockerbox client's rbAdvId spx
        return rbClientSpxDao.updateRbClientAdvIdSpx(variableId, newRbAdvId)
    }

    /**
     * Deletes a Rockerbox client's smart pixels by mntn advertiserId if applicable,
     * and returns a boolean flag indicating the status of action.
     */
    fun deleteRbClient(advertiserId: Int, map: Map<String, AdvertiserSmartPxVariables>): Boolean {
        val pixels = map.values.toList()
        val variableIds = mutableListOf<Int>()

        // Double-check the pixels are rb client spx before deletion.
        // Add the variableId to the deletion list
        pixels.forEach { pixel ->
            val query = pixel.query
            val pixelInfoString = getSpxInfoString(pixel)
            if (pixel.advertiserId != advertiserId) {
                log.error("wrong spx retrieved for advertiserId=[$advertiserId]. mission abandoned. spx=$pixelInfoString")
                return false
            }
            if (rbClientSpxDao.isRbClientSpxByQuery(query)) {
                log.debug("added rb client spx to deletion list for advertiserID=[$advertiserId]... spx=$pixelInfoString")
                variableIds.add(pixel.variableId)
            } else {
                log.error("non rb client spx found to be deleted for advertiserID=[$advertiserId]. mission abandoned. spx=$pixelInfoString")
                return false
            }
        }
        return rbClientSpxDao.deleteSPXsByVariableIds(advertiserId, variableIds)
    }

    private fun retrieveRbAdvIdFromSpxQuery(spx: AdvertiserSmartPxVariables?): String? {
        if (spx == null || spx.query == null) return null
        val regex = Regex("rb_adv_id=(\\w+)")
        val result = findRegexMatchResultInString(regex, spx.query)
        if (result == null) {
            log.error("no rb_adv_id in rb client getRockerBoxAdvID spx. variableId=[${spx.variableId}]; advertiserId=[${spx.advertiserId}]")
            return null
        }
        return result
    }
}
