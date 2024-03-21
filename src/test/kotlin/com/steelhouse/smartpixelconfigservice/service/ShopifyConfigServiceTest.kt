package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.smartpixelconfigservice.datasource.MultipleTablesData
import com.steelhouse.smartpixelconfigservice.datasource.Status
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShopifyConfigServiceTest {
    private val multipleTablesData = mockk<MultipleTablesData>()
    private val shopifyConfigService = ShopifyConfigService(multipleTablesData)

    @Test
    fun `migrateConversionPixel checks number of rows matched in db`() {
        // Case: validates number of rows
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(true, 0, "dummy message") }
        var status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, true)
        assertEquals(status.numOfRowsMatched, 0)
        assertEquals(status.message, "nothing to update")

        // Case: passes exception message from db error
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(false, 100, "dummy message") }
        status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, false)
        assertEquals(status.numOfRowsMatched, 100)
        assertEquals(status.message, "dummy message")

        // Case: happy path
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(true, 100, null) }
        status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, true)
        assertEquals(status.numOfRowsMatched, 100)
        assertEquals(status.message, null)
    }
}
