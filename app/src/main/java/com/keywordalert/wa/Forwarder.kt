package com.keywordalert.wa

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * إرسال الرسائل المطابِقة إلى Telegram و/أو webhook (n8n).
 * يعمل على خيط منفصل حتى لا يعطّل النظام.
 */
object Forwarder {

    private const val TAG = "Forwarder"

    fun forward(settings: SettingsStore, item: MatchItem) {
        val message = buildString {
            append("🔔 تطابق كلمة مفتاحية\n")
            append("التطبيق: ${item.app}\n")
            if (item.title.isNotEmpty()) append("العنوان: ${item.title}\n")
            append("الرسالة: ${item.text}\n")
            append("الكلمات: ${item.keywords}")
        }

        val token = settings.telegramToken
        val chatId = settings.telegramChatId
        if (token.isNotEmpty() && chatId.isNotEmpty()) {
            sendTelegram(token, chatId, message)
        }

        val webhook = settings.webhookUrl
        if (webhook.isNotEmpty()) {
            sendWebhook(webhook, item)
        }
    }

    private fun sendTelegram(token: String, chatId: String, message: String) {
        thread {
            try {
                val text = URLEncoder.encode(message, "UTF-8")
                val urlStr = "https://api.telegram.org/bot$token/sendMessage?chat_id=$chatId&text=$text"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                val code = conn.responseCode
                Log.d(TAG, "Telegram response: $code")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Telegram error: ${e.message}")
            }
        }
    }

    private fun sendWebhook(url: String, item: MatchItem) {
        thread {
            try {
                val payload = JSONObject().apply {
                    put("time", item.time)
                    put("app", item.app)
                    put("title", item.title)
                    put("text", item.text)
                    put("keywords", item.keywords)
                }
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }
                val code = conn.responseCode
                Log.d(TAG, "Webhook response: $code")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Webhook error: ${e.message}")
            }
        }
    }
}
