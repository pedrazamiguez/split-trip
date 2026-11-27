package es.pedrazamiguez.splittrip.domain.exception

class LocalAiModelNotEntitledException(
    message: String = "Local AI model pack requires an active Pro subscription"
) : Exception(message)

class LocalAiModelUnavailableException(
    message: String = "Local AI model pack is not installed or failed to load",
    cause: Throwable? = null
) : Exception(message, cause)
