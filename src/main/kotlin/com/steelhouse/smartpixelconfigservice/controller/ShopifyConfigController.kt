package com.steelhouse.smartpixelconfigservice.controller

import com.steelhouse.smartpixelconfigservice.service.ShopifyConfigService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ResponseBody
class ShopifyConfigController(
    private val shopifyConfigService: ShopifyConfigService
) {
    private val log: Logger = LogManager.getLogger(this.javaClass)

    @RequestMapping(
        value = ["/shopify/migrateConversionPixel/advertisers/{advertiserId}"],
        method = [RequestMethod.PATCH]
    )
    fun migrateConversionPixel(
        @PathVariable("advertiserId") advertiserId: Int
    ): ResponseEntity<String> {
        log.info("got request to migrate shopify conversion pixel. advertiser=[$advertiserId]")

        val responseHeaders = HttpHeaders()
        responseHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)

        val updateStatus = shopifyConfigService.migrateConversionPixel(advertiserId)

        if (!updateStatus.isSuccess) return ResponseEntity.internalServerError().headers(responseHeaders).body(updateStatus.message)

        if (updateStatus.message == null) return ResponseEntity(HttpStatus.OK)

        log.error("client request error: nothing for shopify conversion pixel migration. advertiserId=[$advertiserId].")
        return ResponseEntity.badRequest().headers(responseHeaders).body(updateStatus.message)
    }
}