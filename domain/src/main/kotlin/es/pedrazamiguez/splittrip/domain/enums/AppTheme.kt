package es.pedrazamiguez.splittrip.domain.enums

enum class AppTheme(val code: String, val englishName: String) {
    SYSTEM("system", "System default"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromCode(code: String?): AppTheme {
            if (code != null) {
                return entries.find { it.code.equals(code, ignoreCase = true) } ?: SYSTEM
            }
            return SYSTEM
        }
    }
}
