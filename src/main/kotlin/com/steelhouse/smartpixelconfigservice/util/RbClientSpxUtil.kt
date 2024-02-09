package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables

// Rockerbox client's getRockerBoxUID(uid) spx looks like:
// let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=${document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();
const val rbClientUidSpxFieldQueryKeyword = "getRockerBoxUID()"

// Rockerbox client's getRockerBoxAdvID(advId) spx looks like:
// let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=test_id"; }; getRockerBoxAdvID();
const val rbClientAdvIdSpxFieldQueryKeyword = "getRockerBoxAdvID()"

val rbClientAdvIdExtractorRegex = Regex("rb_adv_id=[\\w-]+")

// Rockerbox advertiser id can contain alphanumeric, underscore: [a-zA-Z0-9_] and hyphen: [-]
val rbClientAdvIdRegex = Regex("[\\w-]+")

fun String.isRbAdvIdValid(): Boolean {
    return this.matches(rbClientAdvIdRegex)
}

fun findRbAdvIdInString(str: String): String? {
    val matchResult = rbClientAdvIdExtractorRegex.find(str) ?: return null
    return matchResult.groupValues[0].split("=")[1]
}

fun String.isRbClientSpx(): Boolean {
    return this.isRbClientAdvIdSpx() || this.isRbClientUidSpx()
}

fun String.isRbClientAdvIdSpx(): Boolean {
    return this.contains(rbClientAdvIdSpxFieldQueryKeyword)
}

fun String.isRbClientUidSpx(): Boolean {
    return this.contains(rbClientUidSpxFieldQueryKeyword)
}

fun createRbClientAdvIdSpx(aid: Int, rbAdvId: String): AdvertiserSmartPxVariables {
    val spx = AdvertiserSmartPxVariables()
    spx.advertiserId = aid
    spx.query = rbAdvId.createRbClientAdvIdSpxFieldQuery()
    return spx
}

fun String.createRbClientAdvIdSpxFieldQuery(): String {
    return """
        let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=$this"; }; getRockerBoxAdvID();
    """.trimIndent()
}

fun Int.createRbClientUidSpx(): AdvertiserSmartPxVariables {
    val spx = AdvertiserSmartPxVariables()
    spx.advertiserId = this
    spx.query = getRbClientUidSpxFieldQuery()
    return spx
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
fun getRbClientUidSpxFieldQuery(): String {
    return rbClientUidSpxFieldQueryPart1 + rbClientUidSpxFieldQueryPart2
}

val rbClientUidSpxFieldQueryPart1 = """
    let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=$
""".trimIndent()

val rbClientUidSpxFieldQueryPart2 = """
    {document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();	
""".trimIndent()
