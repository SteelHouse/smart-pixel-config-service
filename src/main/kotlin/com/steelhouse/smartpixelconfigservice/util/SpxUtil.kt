package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables

fun getSpxListInfoString(list: List<AdvertiserSmartPxVariables>): String {
    var str = ""
    list.forEach { spx -> str += (getSpxInfoString(spx)) }
    return str
}

fun getSpxInfoString(spx: AdvertiserSmartPxVariables): String {
    return "\t{variableId=[${spx.variableId}]; advertiserId=[${spx.advertiserId}]; query=[${spx.query.trimIndent()}]}"
}
