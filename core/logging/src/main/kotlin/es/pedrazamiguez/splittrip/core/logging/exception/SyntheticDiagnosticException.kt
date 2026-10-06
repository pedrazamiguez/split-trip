package es.pedrazamiguez.splittrip.core.logging.exception

class SyntheticDiagnosticException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
