package com.steelhouse.smartpixelconfigservice.util

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables

// Rockerbox client's getRockerBoxUID(uid) spx looks like:
// let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=${document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();
const val rbClientUidSpxFieldQueryKeyword = "getRockerBoxUID()"

// Rockerbox client's getRockerBoxAdvID(advId) spx looks like:
// let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=test_id"; }; getRockerBoxAdvID();
const val rbClientAdvIdSpxFieldQueryKeyword = "getRockerBoxAdvID()"
fun isRbClientSpx(query: String): Boolean {
    return isRbClientAdvIdSpx(query) || isRbClientUidSpx(query)
}

fun isRbClientAdvIdSpx(query: String): Boolean {
    return query.contains(rbClientAdvIdSpxFieldQueryKeyword)
}

fun isRbClientUidSpx(query: String): Boolean {
    return query.contains(rbClientUidSpxFieldQueryKeyword)
}

fun createRbClientAdvIdSpx(aid: Int, rbAdvId: String): AdvertiserSmartPxVariables {
    val spx = AdvertiserSmartPxVariables()
    spx.advertiserId = aid
    spx.query = createRbClientAdvIdSpxFieldQuery(rbAdvId)
    return spx
}

fun createRbClientAdvIdSpxFieldQuery(rbAdvId: String): String {
    return """
        let getRockerBoxAdvID = () => { let rb_adv_id = null; return "rb_adv_id=$rbAdvId"; }; getRockerBoxAdvID();
    """.trimIndent()
}

fun createRbClientUidSpx(aid: Int): AdvertiserSmartPxVariables {
    val spx = AdvertiserSmartPxVariables()
    spx.advertiserId = aid
    spx.query = createRbClientUidSpxFieldQuery()
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
fun createRbClientUidSpxFieldQuery(): String {
    return rbClientUidSpxFieldQueryPart1 + rbClientUidSpxFieldQueryPart2
}

val rbClientUidSpxFieldQueryPart1 = """
    let getRockerBoxUID = () => { let rb_uid = null; try{ rb_uid = `rb_uid=$
""".trimIndent()

val rbClientUidSpxFieldQueryPart2 = """
    {document.cookie.split("rbuid=")[1].split(";")[0].trim()}`; }catch(e){ rb_uid = null }; return rb_uid }; getRockerBoxUID();	
""".trimIndent()
