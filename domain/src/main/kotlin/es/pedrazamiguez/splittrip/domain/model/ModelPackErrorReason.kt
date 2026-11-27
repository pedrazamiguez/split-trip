package es.pedrazamiguez.splittrip.domain.model

enum class ModelPackErrorReason {
    NOT_ENTITLED,
    NETWORK_ERROR,
    WIFI_REQUIRED,
    INSUFFICIENT_STORAGE,
    CHECKSUM_MISMATCH,
    INVALID_CONFIG,
    IO_ERROR,
    CANCELLED,
    RUNTIME_UNAVAILABLE,
    UNKNOWN
}
