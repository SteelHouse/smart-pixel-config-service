package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
class SpxUtilTest {

    @Test
    fun testGetSpxListFieldQueryInfoString() {
        val spx1 = AdvertiserSmartPxVariables()
        spx1.variableId = 123
        spx1.advertiserId = 1234
        spx1.query = "dummy_123"
        val spx2 = AdvertiserSmartPxVariables()
        spx2.variableId = 456
        spx2.advertiserId = 4567
        spx2.query = "dummy_456"
        assertEquals(
            "\t{variableId=[123]; advertiserId=[1234]; query=[dummy_123]}" +
                "\t{variableId=[456]; advertiserId=[4567]; query=[dummy_456]}",
            getSpxListFieldQueryInfoString(listOf(spx1, spx2))
        )
    }

    @Test
    fun testGetSpxFieldQueryInfoString() {
        val spx = AdvertiserSmartPxVariables()
        spx.variableId = 123
        spx.advertiserId = 456
        spx.query = "dummy"
        assertEquals("\t{variableId=[123]; advertiserId=[456]; query=[dummy]}", getSpxFieldQueryInfoString(spx))
    }
}
