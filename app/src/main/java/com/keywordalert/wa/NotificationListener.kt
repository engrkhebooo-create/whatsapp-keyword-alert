package com.keywordalert.wa

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
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

    private val baseChannelId = "wa_keyword_alert_channel"

    // معرّف القناة الحالي يعتمد على نسخة النغمة (يتغيّر عند تغيير النغمة)
    private fun channelId(settings: SettingsStore) = baseChannelId + "_v" + settings.channelVersion

    // لتجنّب تكرار التنبيه لنفس الإشعار بسرعة
    private var lastKey: String = ""
    private var lastTime: Long = 0

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

            // تجاهل الرسالة إذا احتوت إحدى كلمات التجاهل (مثل: معلمة، مدرسة)
            val excluded = TextNormalizer.matchedKeywords(combined, settings.excludeList())
            if (excluded.isNotEmpty()) return

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
        val chId = ensureChannel(settings, nm)
        val builder = NotificationCompat.Builder(this, chId)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("تطابق: ${item.keywords}")
            .setContentText(item.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        nm.notify(item.time.toInt(), builder.build())
    }

    /**
     * ينشئ قناة الإشعار بالنغمة المختارة. بما أن نغمة القناة لا يمكن تغييرها بعد إنشائها،
     * نستخدم معرّفاً يحمل رقم نسخة يتغيّر عند اختيار نغمة جديدة، ونحذف القنوات القديمة.
     */
    private fun ensureChannel(settings: SettingsStore, nm: NotificationManager): String {
        val chId = channelId(settings)
        if (nm.getNotificationChannel(chId) == null) {
            // حذف القنوات القديمة (النسخ السابقة)
            for (ch in nm.notificationChannels) {
                if (ch.id.startsWith(baseChannelId) && ch.id != chId) {
                    nm.deleteNotificationChannel(ch.id)
                }
            }
            val channel = NotificationChannel(
                chId,
                "تنبيهات الكلمات المفتاحية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيه عند تطابق كلمة مفتاحية في رسائل واتساب"
                enableVibration(settings.vibrateEnabled)
                if (settings.soundEnabled) {
                    val uri: Uri = if (settings.soundUri.isNotEmpty()) {
                        Uri.parse(settings.soundUri)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                    val attrs = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                    setSound(uri, attrs)
                } else {
                    setSound(null, null)
                }
            }
            nm.createNotificationChannel(channel)
        }
        return chId
    }
}
