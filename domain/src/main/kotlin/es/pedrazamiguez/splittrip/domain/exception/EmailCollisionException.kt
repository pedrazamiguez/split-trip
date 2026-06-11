package es.pedrazamiguez.splittrip.domain.exception

class EmailCollisionException(
    val email: String,
    cause: Throwable? = null
) : Exception("An account with email $email already exists", cause)
