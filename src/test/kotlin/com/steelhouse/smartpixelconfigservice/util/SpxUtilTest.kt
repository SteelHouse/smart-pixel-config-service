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
        val validStrings = listOf("abcXYZ", "abc123", "123", "abc_123", "abc_123_abc", "abc-123")
        for (str in validStrings) assertTrue(str.isAlphanumericWithUnderscore(), "string $str should be valid")

        val notValidStrings = listOf("abc\"XYZ", "abc@123", "abc.123", "abc 123", "abc/123", "abc\\123", "abc?123", "abc[123", "abc]123")
        for (str in notValidStrings) assertFalse(str.isAlphanumericWithUnderscore(), "string $str should not be valid")
    }

    @Test
    fun testFindRegexMatchResultInString() {
        val regex = rbClientAdvIdExtractorRegex
        Assertions.assertAll(
            { assertNull(findRegexMatchResultInString(regex, "test_id")) },
            { assertNull(findRegexMatchResultInString(regex, "rb_adv_id?test_id")) },
            { assertNull(findRegexMatchResultInString(regex, "rb_adv_id=")) },
            { assertEquals("test-id_test-id", findRegexMatchResultInString(regex, "rb_adv_id=test-id_test-id")) },
            { assertEquals("test-id", findRegexMatchResultInString(regex, "rb_adv_id=test-id?id")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "rb_adv_id=test_id\"\"")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "\"rb_adv_id=test_id")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "return \"rb_adv_id=test_id\"; }")) },
            { assertEquals("test-id_test-id", findRegexMatchResultInString(regex, "return \"rb_adv_id=test-id_test-id\"; }")) },
            { assertEquals("test_id", findRegexMatchResultInString(regex, "return \"rb_adv_id=test_id\"_test_id\"; }")) }
        )
    }

    @Test
    fun testGetSpxListInfoString() {
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
            getSpxListInfoString(listOf(spx1, spx2))
        )
    }

    @Test
    fun testGetSpxInfoString() {
        val spx = AdvertiserSmartPxVariables()
        spx.variableId = 123
        spx.advertiserId = 456
        spx.query = "dummy"
        assertEquals("\t{variableId=[123]; advertiserId=[456]; query=[dummy]}", getSpxInfoString(spx))
    }
}
