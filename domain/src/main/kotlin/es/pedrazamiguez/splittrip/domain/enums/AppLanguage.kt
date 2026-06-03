package es.pedrazamiguez.splittrip.domain.enums

import java.util.Locale

enum class AppLanguage(val code: String, val englishName: String) {
    EN("en", "English"),
    ES("es", "Spanish");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            val targetCode = code ?: Locale.getDefault().language
            return entries.find {
                it.code.equals(targetCode, ignoreCase = true)
            } ?: EN
        }
    }
}
