package com.steelhouse.smartpixelconfigservice.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpxForRbClientUtilTest {

    @Test
    fun testIsRbAdvIdValid() {
        val validStrings = listOf("abcXYZ", "abc123", "123", "abc_123", "abc-123", "abc-123_XYZ-456")
        for (str in validStrings) assertTrue(str.isRbAdvIdValid(), "string $str should be valid")

        val notValidStrings = listOf(
            "~", "`", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "+",
            "{", "}", "[", "]", "|", "\\",
            ":", ";", "\"", "'",
            "<", ">", "?", ",", ".", "/", " "
        )
        for (str in notValidStrings) {
            assertFalse(str.isRbAdvIdValid(), "string $str should not be valid")
        }
    }

    @Test
    fun testFindRegexMatchResultInString() {
        Assertions.assertAll(
            { assertNull(findRbAdvIdInString("test_id")) },
            { assertNull(findRbAdvIdInString("rb_adv_id?test_id")) },
            { assertNull(findRbAdvIdInString("rb_adv_id=")) },
            { assertEquals("test-id_test-id", findRbAdvIdInString("rb_adv_id=test-id_test-id")) },
            { assertEquals("test-id", findRbAdvIdInString("rb_adv_id=test-id?id")) },
            { assertEquals("test_id", findRbAdvIdInString("rb_adv_id=test_id\"\"")) },
            { assertEquals("test_id", findRbAdvIdInString("\"rb_adv_id=test_id")) },
            { assertEquals("test_id", findRbAdvIdInString("return \"rb_adv_id=test_id\"; }")) },
            { assertEquals("test-id_test-id", findRbAdvIdInString("return \"rb_adv_id=test-id_test-id\"; }")) },
            { assertEquals("test_id", findRbAdvIdInString("return \"rb_adv_id=test_id\"_test_id\"; }")) }
        )
    }

    @Test
    fun testIsSpxForRbClient() {
        assertFalse("".isSpxForRbClient())
        assertFalse("dummy".isSpxForRbClient())
        assertFalse("getRockerBoxAdvID".isSpxForRbClient())
        assertFalse("getRockerBoxUID".isSpxForRbClient())
        assertTrue("getRockerBoxAdvID()".isSpxForRbClient())
        assertTrue("getRockerBoxUID()".isSpxForRbClient())
    }

    @Test
    fun testIsSpxForRbClientAdvId() {
        assertFalse("".isSpxForRbClientAdvId())
        assertFalse("dummy".isSpxForRbClientAdvId())
        assertFalse("getRockerBoxAdvID".isSpxForRbClientAdvId())
        assertTrue("getRockerBoxAdvID()".isSpxForRbClientAdvId())
    }

    @Test
    fun testIsSpxForRbClientUid() {
        assertFalse("".isSpxForRbClientUid())
        assertFalse("dummy".isSpxForRbClientUid())
        assertFalse("getRockerBoxUID".isSpxForRbClientUid())
        assertTrue("getRockerBoxUID()".isSpxForRbClientUid())
    }

    @Test
    fun testCreateFieldQueryOfSpxForRbClientAdvId() {
        assertEquals(
            """let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=dummy_id"; }; getRockerBoxAdvID();""",
            "dummy_id".createFieldQueryOfSpxForRbClientAdvId()
        )
    }

    @Test
    fun testGetFieldQueryOfSpxForRbClientUiy() {
        // print(getFieldQueryOfSpxForRbClientUid())
    }
}
