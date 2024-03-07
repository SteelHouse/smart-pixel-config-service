package com.steelhouse.smartpixelconfigservice.service

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import com.steelhouse.postgresql.publicschema.SpxConversionVariables
import com.steelhouse.smartpixelconfigservice.datasource.dao.Spx
import com.steelhouse.smartpixelconfigservice.datasource.dao.SpxConvVar
import org.springframework.stereotype.Component

@Component
class ConfigService(
    private val spx: Spx,
    private val spxConvVar: SpxConvVar
) {
    fun getAdvertiserSmartPxVariablesList(advertiserId: Int, trpxCallParameterDefaultsIdList: List<String>): List<AdvertiserSmartPxVariables>? {
        return spx.getSpxListByAdvertiserId(advertiserId, trpxCallParameterDefaultsIdList)
    }

    fun getSpxConversionVariablesList(advertiserId: Int, variableIdList: List<String>): List<SpxConversionVariables>? {
        return spxConvVar.getSpxConvVarListByAdvertiserId(advertiserId, variableIdList)
    }
}
