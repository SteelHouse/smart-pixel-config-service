package com.steelhouse.smartpixelconfigservice.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RbClientSpxUtilTest {

    @Test
    fun testIsRbClientAdvIdSpx() {
        assertFalse(isRbClientAdvIdSpx(""))
        assertFalse(isRbClientAdvIdSpx("dummy"))
        assertFalse(isRbClientAdvIdSpx("getRockerBoxAdvID"))
        assertTrue(isRbClientAdvIdSpx("getRockerBoxAdvID()"))
    }

    @Test
    fun testIsRbClientUidSpx() {
        assertFalse(isRbClientUidSpx(""))
        assertFalse(isRbClientUidSpx("dummy"))
        assertFalse(isRbClientUidSpx("getRockerBoxUID"))
        assertTrue(isRbClientUidSpx("getRockerBoxUID()"))
    }

    @Test
    fun testCreateRbClientAdvIdSpxFieldQuery() {
        Assertions.assertEquals(
            """let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=dummy_id"; }; getRockerBoxAdvID();""",
            createRbClientAdvIdSpxFieldQuery("dummy_id")
        )
    }

    @Test
    fun testCreateRbClientUidSpxFieldQuery() {
        // print(createRbClientUidSpxFieldQuery())
    }
}
