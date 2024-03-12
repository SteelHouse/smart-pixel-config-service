package com.steelhouse.smartpixelconfigservice.controller

import com.steelhouse.smartpixelconfigservice.datasource.dao.Status
import com.steelhouse.smartpixelconfigservice.service.ShopifyConfigService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class ShopifyConfigControllerTest {
    private val service = mockk<ShopifyConfigService>()
    private val controller = ShopifyConfigController(service)

    @Test
    fun `migrateConversionPixel returns exception message for db error`() {
        val exceptionMsg = "dummy exception message"
        every { service.migrateConversionPixel(1) } answers { Status(false, exceptionMsg) }
        val response = controller.migrateConversionPixel(1)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(exceptionMsg, response.body)
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.headers.contentType.toString())
    }

    @Test
    fun `migrateConversionPixel returns local message from db transaction`() {
        val localMsg = "dummy local message"
        every { service.migrateConversionPixel(1) } answers { Status(true, localMsg) }
        val response = controller.migrateConversionPixel(1)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(localMsg, response.body)
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.headers.contentType.toString())
    }

    @Test
    fun `migrateConversionPixel happy path`() {
        every { service.migrateConversionPixel(1) } answers { Status(true, null) }
        val response = controller.migrateConversionPixel(1)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(null, response.body)
    }
}
