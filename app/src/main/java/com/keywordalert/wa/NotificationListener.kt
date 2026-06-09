package com.keywordalert.wa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/**
 * خدمة قراءة الإشعارات. تستمع لإشعارات واتساب، تبحث عن الكلمات المفتاحية،
 * وعند التطابق: تنبّه (صوت/اهتزاز)، تحفظ الرسالة، وتعيد إرسالها (Telegram/webhook).
 */
class NotificationListener : NotificationListenerService() {

    private val watchedPackages = setOf(
        "com.whatsapp",        // واتساب
        "com.whatsapp.w4b"     // واتساب للأعمال
    )

    private val alertChannelId = "wa_keyword_alert_channel"

    // لتجنّب تكرار التنبيه لنفس الإشعار بسرعة
    private var lastKey: String = ""
    private var lastTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName !in watchedPackages) return

            val settings = SettingsStore(this)
            if (!settings.enabled) return

            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            val combined = listOf(title, text, bigText).filter { it.isNotEmpty() }.joinToString(" \n ")
            if (combined.isBlank()) return

            // تجاهل إشعارات واتساب التجميعية (مثل: 3 رسائل جديدة من محادثتين)
            if (text.contains("رسالة") && text.contains("محادث")) return

            val keywords = settings.keywordsList()
            if (keywords.isEmpty()) return

            val matched = TextNormalizer.matchedKeywords(combined, keywords)
            if (matched.isEmpty()) return

            // منع التكرار خلال 3 ثوانٍ لنفس النص
            val now = System.currentTimeMillis()
            val key = sbn.packageName + "|" + combined
            if (key == lastKey && now - lastTime < 3000) return
            lastKey = key
            lastTime = now

            val appLabel = if (sbn.packageName == "com.whatsapp.w4b") "WhatsApp Business" else "WhatsApp"
            val item = MatchItem(
                time = now,
                app = appLabel,
                title = title,
                text = if (bigText.isNotEmpty()) bigText else text,
                keywords = matched.joinToString("، ")
            )

            // 1) حفظ
            MatchStore(this).add(item)

            // 2) تنبيه (صوت/اهتزاز + إشعار)
            alert(settings, item)

            // 3) إعادة إرسال (اختياري)
            Forwarder.forward(settings, item)

        } catch (e: Exception) {
            // لا نريد إسقاط الخدمة لأي سبب
        }
    }

    private fun alert(settings: SettingsStore, item: MatchItem) {
        // اهتزاز
        if (settings.vibrateEnabled) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (_: Exception) {}
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("تطابق: ${item.keywords}")
            .setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (settings.soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        }

        nm.notify(item.time.toInt(), builder.build())
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(alertChannelId)
        if (existing == null) {
            val channel = NotificationChannel(
                alertChannelId,
                "تنبيهات الكلمات المفتاحية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه عند تطابق كلمة مفتاحية في رسائل واتساب"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
