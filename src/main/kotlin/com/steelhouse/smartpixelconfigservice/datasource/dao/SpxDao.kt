package com.steelhouse.smartpixelconfigservice.datasource.dao

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.smartpixelconfigservice.datasource.repository.SpxRepository

open class SpxDao(
    private val spxRepository: SpxRepository
) {
    fun saveSpx(advertiserSmartPxVariables: AdvertiserSmartPxVariables): AdvertiserSmartPxVariables {
        return spxRepository.save(advertiserSmartPxVariables)
    }

    fun deleteSpxById(variableId: Int) {
        return spxRepository.deleteById(variableId)
    }

    fun deleteSPXsByIds(variableIds: List<Int>) {
        return spxRepository.deleteAllById(variableIds)
    }
}
