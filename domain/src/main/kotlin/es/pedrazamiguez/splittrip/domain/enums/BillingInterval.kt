package es.pedrazamiguez.splittrip.domain.enums

enum class BillingInterval {
    MONTHLY,
    ANNUAL;

    companion object {
        fun fromString(value: String): BillingInterval =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown billing interval: $value")

        fun fromStringOrDefault(value: String?): BillingInterval =
            value?.let { v -> entries.find { it.name.equals(v, ignoreCase = true) } } ?: ANNUAL
    }
}
