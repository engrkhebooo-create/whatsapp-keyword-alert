package com.keywordalert.wa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MatchItem(
    val time: Long,
    val app: String,
    val title: String,
    val text: String,
    val keywords: String
)

/**
 * تخزين الرسائل المطابِقة محلياً (آخر 200 رسالة) في SharedPreferences كـ JSON.
 */
class MatchStore(context: Context) {

    private val prefs = context.getSharedPreferences("wa_keyword_matches", Context.MODE_PRIVATE)

    companion object {
        private const val KEY = "matches"
        private const val MAX = 200
    }

    fun add(item: MatchItem) {
        val arr = readArray()
        val obj = JSONObject().apply {
            put("time", item.time)
            put("app", item.app)
            put("title", item.title)
            put("text", item.text)
            put("keywords", item.keywords)
        }
        // إدراج في البداية (الأحدث أولاً)
        val newArr = JSONArray()
        newArr.put(obj)
        var count = 1
        var i = 0
        while (i < arr.length() && count < MAX) {
            newArr.put(arr.get(i))
            count++
            i++
        }
        prefs.edit().putString(KEY, newArr.toString()).apply()
    }

    fun all(): List<MatchItem> {
        val arr = readArray()
        val list = ArrayList<MatchItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                MatchItem(
                    time = o.optLong("time"),
                    app = o.optString("app"),
                    title = o.optString("title"),
                    text = o.optString("text"),
                    keywords = o.optString("keywords")
                )
            )
        }
        return list
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun readArray(): JSONArray {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}
