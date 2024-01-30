package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
class SpxUtilTest {

    @Test
    fun testIsAlphanumericWithUnderscore() {
        val validStrings = listOf("abcXYZ", "abc123", "123", "abc_123")
        for (str in validStrings) assertTrue(isAlphanumericWithUnderscore(str), "string $str is not valid")

        val notValidStrings = listOf("abc\"XYZ", "abc@123", "abc.123", "abc 123", "abc/123", "abc\\123", "abc?123", "abc[123", "abc]123")
        for (str in notValidStrings) assertFalse(isAlphanumericWithUnderscore(str), "string $str should be valid")
    }

    @Test
    fun testRetrieveRbAdvIdFromSpxQuery() {
        val regex = Regex("rb_adv_id=(\\w+)")
        Assertions.assertAll(
            { assertNull(findRegexMatchResultInString(regex, "test_id")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "\"rb_adv_id=test_id")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "return \"rb_adv_id=test_id\"; }")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "return \"rb_adv_id=test_id\"_test_id\"; }")) }
        )
    }

    @Test
    fun testGetSpxInfoString() {
        val spx = AdvertiserSmartPxVariables()
        spx.variableId = 123
        spx.advertiserId = 456
        spx.query = "dummy"
        assertEquals("\n{variableId=[123]; advertiserId=[456]; query=[dummy]}", getSpxInfoString(spx))
    }

    @Test
    fun testCreateRbClientUidSpxQuery() {
        // print(createRbClientUidSpxQuery())
    }
}
