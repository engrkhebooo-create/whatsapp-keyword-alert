package com.keywordalert.wa

import android.content.Context

/**
 * تخزين إعدادات التطبيق (الكلمات المفتاحية، التفعيل، إعدادات الإرسال) في SharedPreferences.
 */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("wa_keyword_alert", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_KEYWORDS = "keywords"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SOUND = "sound"
        private const val KEY_VIBRATE = "vibrate"
        private const val KEY_TG_TOKEN = "tg_token"
        private const val KEY_TG_CHAT = "tg_chat"
        private const val KEY_WEBHOOK = "webhook"

        // الكلمات المفتاحية الافتراضية لإعلانات التدريس الخصوصي
        val DEFAULT_KEYWORDS = listOf(
            "معلم",
            "معلمة",
            "مدرس",
            "مدرسة",
            "معلم رياضيات",
            "رياضيات",
            "إنجليزي",
            "انجليزي",
            "لغة عربية",
            "فيزياء",
            "كيمياء",
            "علوم",
            "الرياض",
            "أبي معلم",
            "أبغى معلم",
            "ابي معلم",
            "ابغى معلم",
            "أحتاج معلم",
            "مطلوب معلم",
            "تدريس خصوصي",
            "دروس خصوصية",
            "درس خصوصي",
            "تأسيس",
            "متابعة",
            "تحفيظ",
            "قرآن"
        )
    }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var telegramToken: String
        get() = prefs.getString(KEY_TG_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TG_TOKEN, value.trim()).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TG_CHAT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TG_CHAT, value.trim()).apply()

    var webhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEBHOOK, value.trim()).apply()

    /** الكلمات المفتاحية كنص (كل كلمة في سطر). */
    var keywordsRaw: String
        get() = prefs.getString(KEY_KEYWORDS, DEFAULT_KEYWORDS.joinToString("\n"))
            ?: DEFAULT_KEYWORDS.joinToString("\n")
        set(value) = prefs.edit().putString(KEY_KEYWORDS, value).apply()

    fun keywordsList(): List<String> {
        return keywordsRaw.split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
