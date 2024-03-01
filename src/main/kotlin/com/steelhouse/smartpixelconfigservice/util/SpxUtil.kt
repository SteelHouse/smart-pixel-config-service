package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables

fun getSpxListFieldQueryInfoString(list: List<AdvertiserSmartPxVariables>): String {
    var str = ""
    list.forEach { spx -> str += (getSpxFieldQueryInfoString(spx)) }
    return str
}

fun getSpxFieldQueryInfoString(spx: AdvertiserSmartPxVariables): String {
    return "\t{variableId=[${spx.variableId}]; advertiserId=[${spx.advertiserId}]; query=[${spx.query.trimIndent()}]}"
}
