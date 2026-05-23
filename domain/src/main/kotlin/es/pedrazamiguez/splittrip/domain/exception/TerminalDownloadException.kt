package es.pedrazamiguez.splittrip.domain.exception

/**
 * Thrown when a remote receipt download fails with a terminal HTTP response code (e.g. 4xx).
 */
class TerminalDownloadException(
    val responseCode: Int,
    message: String
) : Exception(message)
