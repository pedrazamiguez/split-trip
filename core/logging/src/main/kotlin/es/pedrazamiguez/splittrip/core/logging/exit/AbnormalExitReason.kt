package es.pedrazamiguez.splittrip.core.logging.exit

enum class AbnormalExitReason(val label: String) {
    LOW_MEMORY("REASON_LOW_MEMORY"),
    CRASH_NATIVE("REASON_CRASH_NATIVE"),
    SIGNALED("REASON_SIGNALED")
}
