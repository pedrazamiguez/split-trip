package es.pedrazamiguez.splittrip.domain.exception

class RemoteCurrencyApiException(
    val httpCode: Int,
    override val message: String,
    cause: Throwable? = null
) : Exception(message, cause)
