package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables

// Rockerbox client's getRockerBoxUID(uid) spx looks like:
// let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=${document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();
const val keywordToFindSpxForRbClientUid = "getRockerBoxUID()"

// Rockerbox client's getRockerBoxAdvID(advId) spx looks like:
// let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=test_id"; }; getRockerBoxAdvID();
const val keywordToFindSpxForRbClientAdvId = "getRockerBoxAdvID()"

val rbAdvIdExtractorRegex = Regex("rb_adv_id=[\\w-]+")

// Rockerbox advertiser id can contain alphanumeric, underscore: [a-zA-Z0-9_] and hyphen: [-]
val rbAdvIdRegex = Regex("[\\w-]+")

fun String.isRbAdvIdValid(): Boolean {
    return this.matches(rbAdvIdRegex)
}

fun findRbAdvIdInString(str: String): String? {
    val matchResult = rbAdvIdExtractorRegex.find(str) ?: return null
    return matchResult.groupValues[0].split("=")[1]
}

fun String.isSpxForRbClient(): Boolean {
    return this.isSpxForRbClientAdvId() || this.isSpxForRbClientUid()
}

fun String.isSpxForRbClientAdvId(): Boolean {
    return this.contains(keywordToFindSpxForRbClientAdvId)
}

fun String.isSpxForRbClientUid(): Boolean {
    return this.contains(keywordToFindSpxForRbClientUid)
}

fun createSpxForRbClientAdvId(aid: Int, rbAdvId: String): AdvertiserSmartPxVariables {
    val spx = AdvertiserSmartPxVariables()
    spx.advertiserId = aid
    spx.query = rbAdvId.createFieldQueryOfSpxForRbClientAdvId()
    return spx
}

fun String.createFieldQueryOfSpxForRbClientAdvId(): String {
    return """
        let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=$this"; }; getRockerBoxAdvID();
    """.trimIndent()
}

fun Int.createSpxForRbClientUid(): AdvertiserSmartPxVariables {
    val spx = AdvertiserSmartPxVariables()
    spx.advertiserId = this
    spx.query = getFieldQueryOfSpxForRbClientUid()
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
fun getFieldQueryOfSpxForRbClientUid(): String {
    return fieldQueryOfSpxForRbClientUidPart1 + fieldQueryOfSpxForRbClientUidPart2
}

val fieldQueryOfSpxForRbClientUidPart1 = """
    let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=$
""".trimIndent()

val fieldQueryOfSpxForRbClientUidPart2 = """
    {document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();	
""".trimIndent()
