package es.pedrazamiguez.splittrip.domain.enums

enum class GroupContext {
    TRAVEL,
    EVERYDAY;

    companion object {
        val DEFAULT: GroupContext = TRAVEL

        fun fromStringOrDefault(value: String?): GroupContext =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}
