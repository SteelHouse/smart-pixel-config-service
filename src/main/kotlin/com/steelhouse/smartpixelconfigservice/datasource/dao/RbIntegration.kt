package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.RockerboxIntegration
import com.steelhouse.smartpixelconfigservice.datasource.repository.RbIntegrationRepository
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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
}
