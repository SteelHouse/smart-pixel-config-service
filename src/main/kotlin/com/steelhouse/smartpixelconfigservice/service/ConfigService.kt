package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.postgresql.publicschema.RockerboxIntegration
import com.steelhouse.postgresql.publicschema.SpxConversionVariables
import com.steelhouse.smartpixelconfigservice.datasource.dao.RbIntegration
import com.steelhouse.smartpixelconfigservice.datasource.dao.Spx
import com.steelhouse.smartpixelconfigservice.datasource.dao.SpxConvVar
import org.springframework.stereotype.Component

@Component
class ConfigService(
    private val spx: Spx,
    private val spxConvVar: SpxConvVar,
    private val rbIntegration: RbIntegration
) {
    fun getAdvertiserSmartPxVariablesListByAdvertiserId(advertiserId: Int, trpxCallParameterDefaultsIdList: List<String>): List<AdvertiserSmartPxVariables>? {
        return spx.getSpxListByAdvertiserId(advertiserId, trpxCallParameterDefaultsIdList)
    }

    fun getAdvertiserSmartPxVariablesListByVariableIds(variableIdList: List<String>): List<AdvertiserSmartPxVariables>? {
        return spx.getSpxListByVariableIds(variableIdList.map { it.toInt() })
    }

    fun getSpxConversionVariablesList(advertiserId: Int, variableIdList: List<String>): List<SpxConversionVariables>? {
        return spxConvVar.getSpxConvVarListByAdvertiserId(advertiserId, variableIdList)
    }

    fun getRockerboxIntegrationList(advertiserId: Int?): List<RockerboxIntegration>? {
        return rbIntegration.getRbIntegrationListByAdvertiserId(advertiserId)
    }
}
