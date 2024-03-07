package com.steelhouse.smartpixelconfigservice.controller

import com.steelhouse.smartpixelconfigservice.service.ShopifyConfigService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class ShopifyConfigControllerTest {
    private val service = mockk<ShopifyConfigService>()
    private val controller = ShopifyConfigController(service)

    @Test
    fun `migrateConversionPixel returns exception message for db error`() {
        val exceptionMsg = "dummy exception message"
        every { service.migrateConversionPixel(1) }.throws(Exception(exceptionMsg))
        val response = controller.migrateConversionPixel(1)
        assertEquals(exceptionMsg, response.body)
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.headers.contentType.toString())
    }
}
