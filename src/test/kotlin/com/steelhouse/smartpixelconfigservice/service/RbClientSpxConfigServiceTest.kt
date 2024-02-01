package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.config.RbClientConfig
import com.steelhouse.smartpixelconfigservice.datasource.dao.RbClientSpx
import com.steelhouse.smartpixelconfigservice.datasource.dao.rbClientAdvIdSpxTag
import com.steelhouse.smartpixelconfigservice.datasource.dao.rbClientUidSpxTag
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RbClientSpxConfigServiceTest {
    private val rbClientSpx = mockk<RbClientSpx>()
    private val service = RbClientSpxConfigService(rbClientSpx)

    private val advIdSpx = AdvertiserSmartPxVariables()
    private val uidSpx = AdvertiserSmartPxVariables()
    private val listOfSpx = listOf(uidSpx, advIdSpx)
    private val mapOfSpx = mapOf(rbClientAdvIdSpxTag to advIdSpx, rbClientUidSpxTag to uidSpx)
    private val aid = 123
    private val aidPlaceholder = 0
    private val rbAdvId = "dummy_adv_id"
    private val validAdvIdSpxQuery = "let getRockerBoxAdvID = () => { return \"rb_adv_id=$rbAdvId\"; }; getRockerBoxAdvID();"
    private val validUidSpxQuery = "let getRockerBoxUID = () => {}; getRockerBoxUID();"

    private fun assignDummyValuesToSPXs() {
        advIdSpx.variableId = 1
        advIdSpx.advertiserId = aid
        advIdSpx.query = "dummyAdvIdSpx"
        uidSpx.variableId = 2
        uidSpx.advertiserId = aid
        uidSpx.query = "dummyUidSpx"
    }

    private fun assignValidQueryToSPXs() {
        advIdSpx.query = validAdvIdSpxQuery
        uidSpx.query = validUidSpxQuery
    }

    @Test
    fun `getClientsAdvertiserIds does not validate spx and returns list`() {
        // Test dummy values
        // Prepare data
        assignDummyValuesToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientSpx.getRbClientsUidSpxList() } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(listOf(aid, aid), service.getRbClientsAdvertiserIds())

        // Test Rockerbox client values values
        // Prepare data
        assignValidQueryToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientSpx.getRbClientsUidSpxList() } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(listOf(aid, aid), service.getRbClientsAdvertiserIds())
    }

    @Test
    fun `getClients validates spx and returns map`() {
        // Test dummy values
        // Prepare data
        assignDummyValuesToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientSpx.getRbClientsAdvIdSpxList() } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(emptyMap<String, AdvertiserSmartPxVariables>(), service.getRbClients())

        // Test Rockerbox client values
        // Prepare data
        assignValidQueryToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientSpx.getRbClientsAdvIdSpxList() } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(mapOf(aid to rbAdvId), service.getRbClients())
    }

    @Test
    fun `getRbClientSpxInfoFromDbByAdvertiserId returns null or map`() {
        // Test null pixel
        // Stub behavior for the mocked properties
        every { rbClientSpx.getSpxListByAdvertiserId(any()) } answers { null }
        // Invoke the function and test
        assertNull(service.getRbClientSpxInfoFromDbByAdvertiserId(0))

        // Test empty list of pixels
        // Stub behavior for the mocked properties
        every { rbClientSpx.getSpxListByAdvertiserId(any()) } answers { emptyList() }
        // Invoke the function and test
        assertEquals(emptyMap<String, AdvertiserSmartPxVariables>(), service.getRbClientSpxInfoFromDbByAdvertiserId(0))

        // Test invalid spx
        // Stub behavior for the mocked properties
        assignDummyValuesToSPXs()
        every { rbClientSpx.getSpxListByAdvertiserId(any()) } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(emptyMap<String, AdvertiserSmartPxVariables>(), service.getRbClientSpxInfoFromDbByAdvertiserId(0))

        // Test valid spx
        // Stub behavior for the mocked properties
        assignValidQueryToSPXs()
        every { rbClientSpx.getSpxListByAdvertiserId(any()) } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(mapOfSpx, service.getRbClientSpxInfoFromDbByAdvertiserId(0))
    }

    @Test
    fun `updateRbClient validates spx and returns early if no update is needed`() {
        val config = RbClientConfig(advertiserId = aidPlaceholder, rbAdvId = rbAdvId)

        // Case: map is empty
        assertFalse(service.updateRbClient(config, emptyMap()))

        // Case: map has no getRockerBoxAdvID spx
        val dummyRbAdvIdSpx = AdvertiserSmartPxVariables()
        assertFalse(service.updateRbClient(config, mapOf("dummy" to dummyRbAdvIdSpx)))

        // Case: getRockerBoxAdvID spx has no variableId
        assertFalse(service.updateRbClient(config, mapOf(rbClientAdvIdSpxTag to dummyRbAdvIdSpx)))

        // Case: getRockerBoxAdvID spx has no query
        dummyRbAdvIdSpx.variableId = 0
        assertFalse(service.updateRbClient(config, mapOf(rbClientAdvIdSpxTag to dummyRbAdvIdSpx)))

        // Case: getRockerBoxAdvID spx field query is wrong
        dummyRbAdvIdSpx.query = "dummyQuery"
        assertFalse(service.updateRbClient(config, mapOf(rbClientAdvIdSpxTag to dummyRbAdvIdSpx)))

        // Case: no change is needed
        dummyRbAdvIdSpx.query = validAdvIdSpxQuery
        assertTrue(service.updateRbClient(config, mapOf(rbClientAdvIdSpxTag to dummyRbAdvIdSpx)))
    }

    @Test
    fun `deleteRbClient validates spx and returns early for error`() {
        // Case: advertiserId does not match
        assignDummyValuesToSPXs()
        assertFalse(service.deleteRbClient(aidPlaceholder, mapOfSpx))

        // Case: spx is not rb client spx
        assertFalse(service.deleteRbClient(aid, mapOfSpx))
    }
}
