package es.pedrazamiguez.splittrip.domain.enums

import java.util.Locale

enum class AppLanguage(
    val code: String,
    val englishName: String,
    val baseLanguageCode: String
) {
    EN("en", "English", "en"),
    ES("es", "Spanish", "es"),
    ANDALUZ("es-AN", "Andalûh (EPA)", "es");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            // First, try exact/case-insensitive match on the code if provided
            if (code != null) {
                entries.find { it.code.equals(code, ignoreCase = true) }?.let { return it }
                if (isAndaluzTag(code)) {
                    return ANDALUZ
                }
            }

            // Fallback: check system default locale language tag for Andaluz
            val defaultLocale = Locale.getDefault()
            if (isAndaluzTag(defaultLocale.toLanguageTag())) {
                return ANDALUZ
            }

            // Otherwise, match base language of default locale or code
            val targetLanguage = (code ?: defaultLocale.language).lowercase(Locale.ROOT)
            return entries.find {
                it.code.equals(targetLanguage, ignoreCase = true)
            } ?: EN
        }

        fun resolveBaseLanguageCode(code: String?): String {
            val trimmed = code?.trim().orEmpty()
            if (trimmed.isNotEmpty()) {
                if (isAndaluzTag(trimmed)) {
                    return ES.baseLanguageCode
                }
                return trimmed
                    .substringBefore('-')
                    .substringBefore('_')
                    .lowercase(Locale.ROOT)
                    .ifBlank { EN.baseLanguageCode }
            }

            val defaultLocale = Locale.getDefault()
            if (isAndaluzTag(defaultLocale.toLanguageTag())) {
                return ES.baseLanguageCode
            }
            return defaultLocale.language.lowercase(Locale.ROOT).ifBlank { EN.baseLanguageCode }
        }

        private fun isAndaluzTag(tag: String): Boolean =
            tag.contains("andaluh", ignoreCase = true) ||
                tag.contains("es-rAN", ignoreCase = true) ||
                tag.contains("es-AN", ignoreCase = true)
    }
}
