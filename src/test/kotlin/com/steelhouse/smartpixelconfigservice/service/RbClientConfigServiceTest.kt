package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.postgresql.publicschema.RockerboxIntegration
import com.steelhouse.smartpixelconfigservice.datasource.RbClientData
import com.steelhouse.smartpixelconfigservice.datasource.dao.RbIntegration
import com.steelhouse.smartpixelconfigservice.datasource.dao.Spx
import com.steelhouse.smartpixelconfigservice.model.RbClientConfigRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RbClientConfigServiceTest {
    private val spx = mockk<Spx>()
    private val rbInt = mockk<RbIntegration>()
    private val rbClientData = mockk<RbClientData>()
    private val service = RbClientConfigService(spx, rbInt, rbClientData)

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

    private val rockerboxIntegration = RockerboxIntegration()

    @Test
    fun `getClientsAdvertiserIdListFromUidSpx does not validate spx`() {
        // Test null value
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsUidSpxList() } answers { null }
        // Invoke the function and test
        assertNull(service.getRbClientsAdvertiserIdListFromUidSpx())

        // Test dummy values
        // Prepare data
        assignDummyValuesToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsUidSpxList() } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(listOf(aid, aid), service.getRbClientsAdvertiserIdListFromUidSpx())

        // Test Rockerbox client values values
        // Prepare data
        assignValidQueryToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsUidSpxList() } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(listOf(aid, aid), service.getRbClientsAdvertiserIdListFromUidSpx())
    }

    @Test
    fun `getClients validates spx`() {
        // Test null value
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { null }
        every { rbClientData.getRbClientsUidSpxList() } answers { listOf(uidSpx) }
        assertNull(service.getRbClients())

        // Test null value
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(advIdSpx) }
        every { rbClientData.getRbClientsUidSpxList() } answers { null }
        assertNull(service.getRbClients())

        // Test dummy values
        // Prepare data
        assignDummyValuesToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(advIdSpx) }
        every { rbClientData.getRbClientsUidSpxList() } answers { listOf(uidSpx) }
        // Invoke the function and test
        assertEquals(emptyMap<Int, String>(), service.getRbClients())

        // Test Rockerbox client values
        // Prepare data
        assignValidQueryToSPXs()
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(advIdSpx) }
        every { rbClientData.getRbClientsUidSpxList() } answers { listOf(uidSpx) }
        // Invoke the function and test
        assertEquals(mapOf(aid to rbAdvId), service.getRbClients())
    }

    @Test
    fun `getClients filters out wrong rb client`() {
        assignDummyValuesToSPXs()
        assignValidQueryToSPXs()
        val dummySpx = AdvertiserSmartPxVariables()
        dummySpx.advertiserId = 1

        // Test rb Client which has getRockerBoxAdvID spx but no getRockerBoxUID spx
        // Prepare data
        dummySpx.query = validAdvIdSpxQuery
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(advIdSpx, dummySpx) }
        every { rbClientData.getRbClientsUidSpxList() } answers { listOf(uidSpx) }
        // Invoke the function and test
        assertEquals(mapOf(aid to rbAdvId), service.getRbClients())

        // Test rb Client which has getRockerBoxUID spx but no getRockerBoxAdvID spx
        // Prepare data
        dummySpx.query = validUidSpxQuery
        // Stub behavior for the mocked properties
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(advIdSpx) }
        every { rbClientData.getRbClientsUidSpxList() } answers { listOf(uidSpx, dummySpx) }
        // Invoke the function and test
        assertEquals(mapOf(aid to rbAdvId), service.getRbClients())
    }

    @Test
    fun `getValidRbClientInfoMap keeps valid rb client and returns map`() {
        assignDummyValuesToSPXs()
        assignValidQueryToSPXs()
        val dummySpx = AdvertiserSmartPxVariables()
        dummySpx.advertiserId = 1

        // Test rb Client which has getRockerBoxAdvID spx but no getRockerBoxUID spx
        // Prepare data
        dummySpx.query = validAdvIdSpxQuery
        val list = mutableListOf<Int>()
        list.add(advIdSpx.advertiserId)
        // Invoke the function and test
        // Error message should be like "wrong rb client found in db: advertiser has getRockerBoxAdvID spx but no getRockerBoxUID spx. advertiserId=[1]"
        assertEquals(mapOf(aid to rbAdvId), service.getValidRbClientInfoMap(listOf(advIdSpx, dummySpx), list))

        // Prepare data and test again
        list.add(dummySpx.advertiserId)
        // Error message should be like "wrong rb client found in db: advertiser has getRockerBoxAdvID spx but no getRockerBoxUID spx. advertiserId=[123]"
        assertEquals(mapOf(dummySpx.advertiserId to rbAdvId), service.getValidRbClientInfoMap(listOf(advIdSpx, dummySpx), list))
    }

    @Test
    fun `getRbClientSpxMapByAdvertiserId returns null or empty map if rockerbox_integration is not retrieved correctly`() {
        // Test null rockerbox_integration
        // Stub behavior for the mocked properties
        every { rbInt.getRbIntegrationListByAdvertiserId(any()) } answers { null }
        // Invoke the function and test
        assertNull(service.getRbClientSpxMapByAdvertiserId(0))

        // Test empty list of rockerbox_integration
        // Stub behavior for the mocked properties
        every { rbInt.getRbIntegrationListByAdvertiserId(any()) } answers { emptyList() }
        // Invoke the function and test
        assertEquals(emptyMap<String, AdvertiserSmartPxVariables>(), service.getRbClientSpxMapByAdvertiserId(0))

        // Test invalid list of rockerbox_integration
        // Stub behavior for the mocked properties
        every { rbInt.getRbIntegrationListByAdvertiserId(any()) } answers { listOf(rockerboxIntegration, rockerboxIntegration) }
        // Invoke the function and test
        assertNull(service.getRbClientSpxMapByAdvertiserId(0))
    }

    @Test
    fun `getRbClientSpxMapByAdvertiserId returns null if advertiser_smart_px_variables is not retrieved correctly`() {
        every { rbInt.getRbIntegrationListByAdvertiserId(any()) } answers { listOf(rockerboxIntegration) }

        // Test null spx
        // Stub behavior for the mocked properties
        every { spx.getSpxListByVariableIds(any()) } answers { null }
        // Invoke the function and test
        assertNull(service.getRbClientSpxMapByAdvertiserId(0))

        // Test empty list of spx
        // Stub behavior for the mocked properties
        every { spx.getSpxListByVariableIds(any()) } answers { emptyList() }
        // Invoke the function and test
        assertNull(service.getRbClientSpxMapByAdvertiserId(0))
    }

    @Test
    fun `getRbClientSpxMapByAdvertiserId return null or map when spx is retrieved`() {
        every { rbInt.getRbIntegrationListByAdvertiserId(any()) } answers { listOf(rockerboxIntegration) }

        // Test invalid spx
        // Stub behavior for the mocked properties
        assignDummyValuesToSPXs()
        every { spx.getSpxListByVariableIds(any()) } answers { listOfSpx }
        // Invoke the function and test
        assertNull(service.getRbClientSpxMapByAdvertiserId(0))

        // Test valid spx
        // Stub behavior for the mocked properties
        assignValidQueryToSPXs()
        every { spx.getSpxListByVariableIds(any()) } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(mapOfSpx, service.getRbClientSpxMapByAdvertiserId(0))
    }

    @Test
    fun `getRbClientSpxInfoFromSpxTableByAdvertiserId returns null or map`() {
        // Test null spx
        // Stub behavior for the mocked properties
        every { spx.getSpxListByAdvertiserId(any()) } answers { null }
        // Invoke the function and test
        assertNull(service.getRbClientSpxInfoFromSpxTableByAdvertiserId(0))

        // Test empty list of spx
        // Stub behavior for the mocked properties
        every { spx.getSpxListByAdvertiserId(any()) } answers { emptyList() }
        // Invoke the function and test
        assertEquals(emptyMap<String, AdvertiserSmartPxVariables>(), service.getRbClientSpxInfoFromSpxTableByAdvertiserId(0))

        // Test invalid spx
        // Stub behavior for the mocked properties
        assignDummyValuesToSPXs()
        every { spx.getSpxListByAdvertiserId(any()) } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(emptyMap<String, AdvertiserSmartPxVariables>(), service.getRbClientSpxInfoFromSpxTableByAdvertiserId(0))

        // Test valid spx
        // Stub behavior for the mocked properties
        assignValidQueryToSPXs()
        every { spx.getSpxListByAdvertiserId(any()) } answers { listOfSpx }
        // Invoke the function and test
        assertEquals(mapOfSpx, service.getRbClientSpxInfoFromSpxTableByAdvertiserId(0))
    }

    @Test
    fun testIsRbAdvIdUniqueInSpxTable() {
        val config = RbClientConfigRequest(advertiserId = 1, rbAdvId = "test")
        val dbSpx = AdvertiserSmartPxVariables()

        // Case: null from db
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { null }
        assertFalse(service.isRbAdvIdUniqueInSpxTable(config))

        // Case: empty list from db
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { emptyList() }
        assertTrue(service.isRbAdvIdUniqueInSpxTable(config))

        // Case: db spx has no rbAdvId in query
        dbSpx.variableId = 1
        dbSpx.advertiserId = 1
        dbSpx.query = ""
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(dbSpx) }
        assertTrue(service.isRbAdvIdUniqueInSpxTable(config))

        // Case: db spx has same advertiserId
        dbSpx.query = "\"rb_adv_id=test"
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(dbSpx) }
        assertTrue(service.isRbAdvIdUniqueInSpxTable(config))

        // Case: db spx has different advertiserId and same rbAdvId
        dbSpx.advertiserId = 2
        every { rbClientData.getRbClientsAdvIdSpxList() } answers { listOf(dbSpx) }
        assertFalse(service.isRbAdvIdUniqueInSpxTable(config))
    }

    @Test
    fun `updateRbClient validates spx and returns early if no update is needed`() {
        val config = RbClientConfigRequest(advertiserId = aidPlaceholder, rbAdvId = rbAdvId)

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
    fun `deleteRbClientSPXs validates spx and returns early for error`() {
        // Case: advertiserId does not match
        assignDummyValuesToSPXs()
        assertFalse(service.deleteRbClientSPXs(aidPlaceholder, mapOfSpx))

        // Case: spx is not rb client spx
        assertFalse(service.deleteRbClientSPXs(aid, mapOfSpx))
    }
}
