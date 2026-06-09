package com.keywordalert.wa

/**
 * أدوات تطبيع النص العربي لمطابقة أدق للكلمات المفتاحية.
 * يوحّد أشكال الألف والياء والتاء المربوطة، ويزيل التشكيل والتطويل.
 */
object TextNormalizer {

    private val diacritics = Regex("[\\u064B-\\u0652\\u0670\\u0640]") // تشكيل + تطويل

    fun normalize(input: String): String {
        var s = input.lowercase().trim()
        s = diacritics.replace(s, "")
        s = s.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace('ة', 'ه')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
        // توحيد المسافات
        s = s.replace(Regex("\\s+"), " ")
        return s
    }

    /**
     * يعيد قائمة الكلمات المفتاحية المطابِقة الموجودة داخل النص.
     */
    fun matchedKeywords(text: String, keywords: List<String>): List<String> {
        val normText = normalize(text)
        val result = ArrayList<String>()
        for (kw in keywords) {
            val nkw = normalize(kw)
            if (nkw.isNotEmpty() && normText.contains(nkw)) {
                result.add(kw.trim())
            }
        }
        return result
    }
}
