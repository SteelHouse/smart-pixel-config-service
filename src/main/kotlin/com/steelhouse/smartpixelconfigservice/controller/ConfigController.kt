package com.steelhouse.smartpixelconfigservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.steelhouse.smartpixelconfigservice.service.ConfigService
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ResponseBody
class ConfigController(
    private val configService: ConfigService
) {
    private val log: Logger = LogManager.getLogger(this.javaClass)

    val validNumberPattern = Regex("^[0-9,]*$")

    @RequestMapping(
        value = ["/advSpxVar/advertisers/{advertiserId}"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getSpxListByAdvertiser(
        @PathVariable("advertiserId") advertiserId: Int,
        @RequestParam(name = "trpxCallParameterDefaultsId", required = false) trpxCallParameterDefaultsId: String?
    ): ResponseEntity<String> {
        // This is a test endpoint for development purpose. The client does not need this endpoint.
        log.info("got request to get all the advertiser_smart_px_variables for advertiser=[$advertiserId], trpxCallParameterDefaultsId=[$trpxCallParameterDefaultsId]")

        val trpxCallParameterDefaultsIdList = trpxCallParameterDefaultsId?.takeIf { validNumberPattern.matches(it) }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()
        val result = configService.getAdvertiserSmartPxVariablesListByAdvertiserId(advertiserId, trpxCallParameterDefaultsIdList) ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(ObjectMapper().writeValueAsString(result))
    }

    @RequestMapping(
        value = ["/advSpxVar"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getSpxListByVariableId(
        @RequestParam(name = "variableId", required = false) variableId: String?
    ): ResponseEntity<String> {
        // This is a test endpoint for development purpose. The client does not need this endpoint.
        log.info("got request to get all the advertiser_smart_px_variables for variableId=[$variableId]")

        val variableIdList = variableId?.takeIf { validNumberPattern.matches(it) }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()
        val result = configService.getAdvertiserSmartPxVariablesListByVariableIds(variableIdList) ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(ObjectMapper().writeValueAsString(result))
    }

    @RequestMapping(
        value = ["/spxConvVar/advertisers/{advertiserId}"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getSpxConvVarListByAdvertiser(
        @PathVariable("advertiserId") advertiserId: Int,
        @RequestParam(name = "variableId", required = false) variableId: String?
    ): ResponseEntity<String> {
        // This is a test endpoint for development purpose. The client does not need this endpoint.
        log.info("got request to get all the spx_conversion_variables for advertiserId=[$advertiserId], variableId=[$variableId]")

        val variableIdList = variableId?.takeIf { validNumberPattern.matches(it) }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
            ?: emptyList()
        val result = configService.getSpxConversionVariablesList(advertiserId, variableIdList) ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(ObjectMapper().writeValueAsString(result))
    }

    @RequestMapping(
        value = ["/rbIntegration/advertisers"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getRbIntegrationList(): ResponseEntity<String> {
        // This is a test endpoint for development purpose. The client does not need this endpoint.
        log.info("got request to get rockerbox_integration")

        val result = configService.getRockerboxIntegrationList(null) ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(ObjectMapper().writeValueAsString(result))
    }

    @RequestMapping(
        value = ["/rbIntegration/advertisers/{advertiserId}"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getRbIntegrationListByAdvertiser(
        @PathVariable("advertiserId") advertiserId: Int
    ): ResponseEntity<String> {
        // This is a test endpoint for development purpose. The client does not need this endpoint.
        log.info("got request to get rockerbox_integration for advertiserId=[$advertiserId]")

        val result = configService.getRockerboxIntegrationList(advertiserId) ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(ObjectMapper().writeValueAsString(result))
    }
}
