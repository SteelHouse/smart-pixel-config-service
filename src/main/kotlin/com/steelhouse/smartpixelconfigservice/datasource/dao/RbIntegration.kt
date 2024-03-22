package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.RockerboxIntegration
import com.steelhouse.smartpixelconfigservice.datasource.repository.RbIntegrationRepository
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
class RbIntegration(
    private val sqlCounter: Counter,
    private val rbIntegration: RbIntegrationRepository
) {

    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    fun getRbIntegrationListByAdvertiserId(advertiserId: Int?): List<RockerboxIntegration>? {
        return try {
            if (advertiserId != null) {
                listOfNotNull(rbIntegration.findById(advertiserId).orElse(null))
            } else {
                rbIntegration.findAll().toList()
            }
        } catch (e: Exception) {
            log.error("unknown db exception to get data. error message=[${e.message}]")
            sqlCounter.labels("rockerbox_integration", "select", "error").inc()
            null
        }
    }

    fun insertRbIntegration(aid: Int, rbAdvId: String, rbAdvIdSpxVariableId: Int, rbUidSpxVariableId: Int): Boolean {
        val newIntegration = RockerboxIntegration()
        newIntegration.advertiserId = aid
        newIntegration.rbAdvId = rbAdvId
        newIntegration.rbAdvIdMappingVariableId = rbAdvIdSpxVariableId
        newIntegration.rbUidMappingVariableId = rbUidSpxVariableId
        newIntegration.createTime = Timestamp(System.currentTimeMillis())
        newIntegration.updateTime = Timestamp(System.currentTimeMillis())
        return try {
            rbIntegration.save(newIntegration)
            log.debug("rockerbox_integration for advertiserId=[$aid] has been created")
            true
        } catch (e: Exception) {
            log.error("unknown db exception to get data. error message=[${e.message}]")
            sqlCounter.labels("rockerbox_integration", "insert", "error").inc()
            false
        }
    }

    fun getAllRbAdvIds(): List<String>? {
        return try {
            val allRbAdvIds = rbIntegration.findAll().toList().map { it.rbAdvId }
            log.debug("existing rb adv ids in database: [${allRbAdvIds.joinToString()}]")
            allRbAdvIds
        } catch (e: Exception) {
            log.error("unknown db exception to get data. error message=[${e.message}]")
            sqlCounter.labels("rockerbox_integration", "select", "error").inc()
            null
        }
    }
}
