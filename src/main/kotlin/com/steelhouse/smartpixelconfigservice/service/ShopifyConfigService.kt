package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.smartpixelconfigservice.datasource.dao.MultipleTables
import org.springframework.stereotype.Component

@Component
class ShopifyConfigService(
    private val multipleTables: MultipleTables
) {
    fun migrateConversionPixel(advertiserId: Int): Boolean? {
        val queryList = createQueryListForMigration(advertiserId)
        return multipleTables.updateMultipleTables(queryList)
    }

    /**
     * The requirements to migrate shopify conversion pixel:
     *
     * For advertiser in advertiser_smart_px_variables table:
     * - set active column to false for trpx_call_parameter_defaults_id = 6 and 11
     * - set active column to false for trpx_call_parameter_defaults_id = 34 AND query contains "run_shopify_conversion_block"
     *
     * For advertiser in spx_conversion_variables table:
     * - set ignore_request_value column to false for variable_id = 6 and 11
     */
    fun createQueryListForMigration(advertiserId: Int): List<String> {
        val queryToUpdateSpx = """
           UPDATE advertiser_smart_px_variables
           SET active = CASE 
               WHEN trpx_call_parameter_defaults_id IN ('6', '11') THEN false 
               WHEN trpx_call_parameter_defaults_id = '34' AND query ILIKE '%run_shopify_conversion_block%' THEN false
               ELSE active 
           END
           WHERE advertiser_id = '$advertiserId';
        """.trimIndent()
        val queryToUpdateSpxConvVar = """
           UPDATE spx_conversion_variables
           SET ignore_request_value = false
           WHERE advertiser_id = '$advertiserId' and variable_id IN ('6', '11');
        """.trimIndent()
        return listOf(queryToUpdateSpx, queryToUpdateSpxConvVar)
    }
}
