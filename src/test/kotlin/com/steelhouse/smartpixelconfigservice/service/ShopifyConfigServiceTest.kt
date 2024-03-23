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
        // Case: validates number of rows for successful update
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(true, 0, "dummy message") }
        var status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, true)
        assertEquals(status.numOfRowsMatched, 0)
        assertEquals(status.message, "nothing to update")

        // Case: passes exception message from db error for failed db update
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(false, 100, "exception message") }
        status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, false)
        assertEquals(status.numOfRowsMatched, 100)
        assertEquals(status.message, "exception message")

        // Case: does not overwrite exception message for failed db update
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(false, 0, "exception message") }
        status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, false)
        assertEquals(status.numOfRowsMatched, 0)
        assertEquals(status.message, "exception message")

        // Case: happy path
        every { multipleTablesData.updateMultipleTables(shopifyConfigService.createQueryListForMigration(1)) } answers { Status(true, 100, null) }
        status = shopifyConfigService.migrateConversionPixel(1)
        assertEquals(status.isExecuted, true)
        assertEquals(status.numOfRowsMatched, 100)
        assertEquals(status.message, null)
    }
}
