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

fun createRbClientAdvIdSpxFieldQuery(rbAdvId: String): String {
    return """
        let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=$rbAdvId"; }; getRockerBoxAdvID();
    """.trimIndent()
}

/**
 * Returns Rockerbox client getRockerBoxUID spx's query.
 * The query looks like below and has to be concatenated by two parts because of character escaping problem.
 *
 * let getRockerBoxUID = () => {
 *     let rb_uid = null;
 *     try{ rb_uid = `rb_uid=${document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }
 *     catch(e){ rb_uid = null };
 *     return rb_uid
 * };
 * getRockerBoxUID();
 */
fun createRbClientUidSpxFieldQuery(): String {
    return rbClientUidSpxFieldQueryPart1 + rbClientUidSpxFieldQueryPart2
}

val rbClientUidSpxFieldQueryPart1 = """
    let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=$
""".trimIndent()

val rbClientUidSpxFieldQueryPart2 = """
    {document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();	
""".trimIndent()
