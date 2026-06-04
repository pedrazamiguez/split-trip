package es.pedrazamiguez.splittrip.domain.enums

import java.util.Locale

enum class AppLanguage(val code: String, val englishName: String) {
    EN("en", "English"),
    ES("es", "Spanish"),
    ANDALUZ("es-AN", "Andalûh (EPA)");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            // First, try exact/case-insensitive match on the code if provided
            if (code != null) {
                entries.find { it.code.equals(code, ignoreCase = true) }?.let { return it }
                if (code.contains("andaluh", ignoreCase = true) ||
                    code.contains("es-rAN", ignoreCase = true) ||
                    code.contains("es-AN", ignoreCase = true)
                ) {
                    return ANDALUZ
                }
            }

            // Fallback: check system default locale language tag for Andaluz
            val defaultTag = Locale.getDefault().toLanguageTag()
            if (defaultTag.contains("andaluh", ignoreCase = true) ||
                defaultTag.contains("es-rAN", ignoreCase = true) ||
                defaultTag.contains("es-AN", ignoreCase = true)
            ) {
                return ANDALUZ
            }

            // Otherwise, match base language of default locale or code
            val targetLanguage = (code ?: Locale.getDefault().language).lowercase()
            return entries.find {
                it.code.equals(targetLanguage, ignoreCase = true)
            } ?: EN
        }
    }
}
