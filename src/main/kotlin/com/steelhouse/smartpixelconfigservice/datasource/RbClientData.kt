package com.steelhouse.smartpixelconfigservice.datasource

import com.steelhouse.smartpixelconfigservice.datasource.repository.RbIntegrationRepository
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository
import com.steelhouse.smartpixelconfigservice.util.createFieldQueryOfSpxForRbClientAdvId
import io.prometheus.client.Counter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RbClientData(
    private val sqlCounter: Counter,
    private val spxRepository: SpxRepository,
    private val rbIntegrationRepository: RbIntegrationRepository,
    private val multipleTablesData: MultipleTablesData
) {
    private val log: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun deleteRbClient(advertiserId: Int, variableIds: List<Int>): Boolean {
        return try {
            spxRepository.deleteAllById(variableIds)
            rbIntegrationRepository.deleteById(advertiserId)
            true
        } catch (e: Exception) {
            // Rollback transaction if any update fails
            log.error("unknown db exception to batch update multiple tables. error message=[${e.message}]")
            sqlCounter.labels("multiple_tables", "batch_update", "error").inc()
            false
        }
    }

    fun updateRbClientRbAdvId(advertiserId: Int, newRbAdvId: String, variableId: Int): Boolean {
        val queryList = createQueryListToUpdateRbAdvId(advertiserId, newRbAdvId, variableId)
        val updateStatus = multipleTablesData.updateMultipleTables(queryList)
        log.debug("${updateStatus.numOfRowsMatched} rows updated") // expect to be 2
        if (updateStatus.numOfRowsMatched != 2) {
            log.error("wrong rb client found in db: rockerbox_integration and spx do not match. advertiserId=[$advertiserId]")
        }
        return updateStatus.isExecuted
    }

    /**
     * Creates queries to update Rockerbox client's rockerbox advertiser id (rb_adv_id).
     */
    fun createQueryListToUpdateRbAdvId(advertiserId: Int, newRbAdvId: String, variableId: Int): List<String> {
        val query = newRbAdvId.createFieldQueryOfSpxForRbClientAdvId()
        val queryToUpdateSpx = """
           UPDATE advertiser_smart_px_variables
           SET query = '$query'
           WHERE variable_id = '$variableId'
        """.trimIndent()
        val queryToUpdateRbIntegration = """
           UPDATE rockerbox_integration
           SET rb_adv_id = '$newRbAdvId', update_time = current_timestamp
           WHERE advertiser_id = '$advertiserId';
        """.trimIndent()
        return listOf(queryToUpdateSpx, queryToUpdateRbIntegration)
    }
}
