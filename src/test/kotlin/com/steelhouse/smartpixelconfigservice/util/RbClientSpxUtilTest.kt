package com.steelhouse.smartpixelconfigservice.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RbClientSpxUtilTest {

    @Test
    fun testIsRbClientSpx() {
        assertFalse("".isRbClientSpx())
        assertFalse("dummy".isRbClientSpx())
        assertFalse("getRockerBoxAdvID".isRbClientSpx())
        assertFalse("getRockerBoxUID".isRbClientSpx())
        assertTrue("getRockerBoxAdvID()".isRbClientSpx())
        assertTrue("getRockerBoxUID()".isRbClientSpx())
    }

    @Test
    fun testIsRbClientAdvIdSpx() {
        assertFalse("".isRbClientAdvIdSpx())
        assertFalse("dummy".isRbClientAdvIdSpx())
        assertFalse("getRockerBoxAdvID".isRbClientAdvIdSpx())
        assertTrue("getRockerBoxAdvID()".isRbClientAdvIdSpx())
    }

    @Test
    fun testIsRbClientUidSpx() {
        assertFalse("".isRbClientUidSpx())
        assertFalse("dummy".isRbClientUidSpx())
        assertFalse("getRockerBoxUID".isRbClientUidSpx())
        assertTrue("getRockerBoxUID()".isRbClientUidSpx())
    }

    @Test
    fun testCreateRbClientAdvIdSpxFieldQuery() {
        Assertions.assertEquals(
            """let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=dummy_id"; }; getRockerBoxAdvID();""",
            "dummy_id".createRbClientAdvIdSpxFieldQuery()
        )
    }

    @Test
    fun testCreateRbClientUidSpxFieldQuery() {
        // print(getRbClientUidSpxFieldQuery())
    }
}
