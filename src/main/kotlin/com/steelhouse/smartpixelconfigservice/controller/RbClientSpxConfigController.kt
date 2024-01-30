package com.steelhouse.smartpixelconfigservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.steelhouse.smartpixelconfigservice.model.RbClientConfig
import com.steelhouse.smartpixelconfigservice.service.RbClientSpxConfigService
import com.steelhouse.smartpixelconfigservice.util.isAlphanumericWithUnderscore
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ResponseBody
class RbClientSpxConfigController(
    private val rbClientSpxConfigService: RbClientSpxConfigService
) {

    private val log: Logger = LogManager.getLogger(this.javaClass)

    @RequestMapping(
        value = ["/spx/rb/advertisers"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAllRockerboxAdvertisers(): ResponseEntity<List<Int>> {
        log.debug("got request to get all the rb clients' advertiser IDs")

        val allRbClientAdvertiserIds = rbClientSpxConfigService.getRbClientsAdvertiserIds() ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(allRbClientAdvertiserIds)
    }

    @RequestMapping(
        value = ["/spx/rb/clients"],
        method = [RequestMethod.GET],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getAllRockerboxClients(): ResponseEntity<String> {
        log.debug("got request to get all the rb clients' advertiserId and rockerboxAdvertiserId")

        val allRbClients = rbClientSpxConfigService.getRbClients() ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        return ResponseEntity.ok().body(ObjectMapper().writeValueAsString(allRbClients))
    }

    @RequestMapping(
        value = ["/spx/rb/advertisers/{advertiserId}"],
        method = [RequestMethod.PUT]
    )
    fun upsertRockerboxClient(
        @PathVariable("advertiserId") advertiserIdInPath: Int,
        @RequestBody rbClientConfig: RbClientConfig
    ): ResponseEntity<String> {
        val advertiserId = rbClientConfig.advertiserId
        val rbAdvId = rbClientConfig.rbAdvId
        log.debug("got request to upsert rb client. advertiser=[$advertiserId]; rbAdvId=[$rbAdvId]")

        if (advertiserId != advertiserIdInPath || isAlphanumericWithUnderscore(rbAdvId)) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }

        val rbClientSpxMap = rbClientSpxConfigService.getRbClientSpxMapByAdvertiserId(advertiserId)
            ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        return if (rbClientSpxMap.isEmpty()) {
            val creationSucceed = rbClientSpxConfigService.createRbClient(rbClientConfig)
            if (creationSucceed) ResponseEntity(HttpStatus.CREATED) else ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        } else {
            val updateSucceed = rbClientSpxConfigService.updateRbClient(rbClientConfig, rbClientSpxMap)
            if (updateSucceed) ResponseEntity(HttpStatus.OK) else ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @RequestMapping(
        value = ["/spx/rb/advertisers/{advertiserId}"],
        method = [RequestMethod.DELETE]
    )
    fun deleteRockerboxClientByAdvertiserId(
        @PathVariable("advertiserId") advertiserId: Int
    ): ResponseEntity<String> {
        log.debug("got request to delete rb client. advertiserId=[$advertiserId]")

        val rbClientSpxMap = rbClientSpxConfigService.getRbClientSpxMapByAdvertiserId(advertiserId)
            ?: return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)

        if (rbClientSpxMap.isEmpty()) return ResponseEntity(HttpStatus.NO_CONTENT)

        val deletionSucceed = rbClientSpxConfigService.deleteRbClient(advertiserId, rbClientSpxMap)
        return if (deletionSucceed) ResponseEntity(HttpStatus.OK) else ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
