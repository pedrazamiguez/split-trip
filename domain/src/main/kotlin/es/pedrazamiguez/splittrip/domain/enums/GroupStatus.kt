package es.pedrazamiguez.splittrip.domain.enums

enum class GroupStatus {
    ACTIVE,
    ARCHIVED;

    companion object {
        fun fromStringOrDefault(value: String?): GroupStatus {
            return entries.find { it.name == value } ?: ACTIVE
        }
    }
}
