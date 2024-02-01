package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables

fun isAlphanumericWithUnderscore(input: String): Boolean {
    val regex = Regex("\\w+")
    return input.matches(regex)
}
fun findRegexMatchResultInString(regex: Regex, str: String): String? {
    val matchResult = regex.find(str) ?: return null
    return matchResult.groupValues[1]
}

fun getSpxListInfoString(list: List<AdvertiserSmartPxVariables>): String {
    var str = ""
    list.forEach { spx -> str += (getSpxInfoString(spx)) }
    return str
}

fun getSpxInfoString(spx: AdvertiserSmartPxVariables): String {
    return "\n{variableId=[${spx.variableId}]; advertiserId=[${spx.advertiserId}]; query=[${spx.query.trimIndent()}]}"
}
