package es.pedrazamiguez.splittrip.domain.enums

enum class AppLanguage(val code: String, val englishName: String) {
    EN("en", "English"),
    ES("es", "Spanish");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.find {
            it.code.equals(code, ignoreCase = true)
        } ?: EN
    }
}
