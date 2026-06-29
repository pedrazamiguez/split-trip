package es.pedrazamiguez.splittrip.domain.enums

enum class PaymentStatus {
    RECEIVED,
    PENDING,
    FINISHED,
    SCHEDULED,
    CANCELLED,
    REFUNDABLE;

    companion object {
        fun fromString(status: String): PaymentStatus = entries.find {
            it.name.equals(
                status,
                ignoreCase = true
            )
        } ?: throw IllegalArgumentException("Unknown status: $status")
    }
}
