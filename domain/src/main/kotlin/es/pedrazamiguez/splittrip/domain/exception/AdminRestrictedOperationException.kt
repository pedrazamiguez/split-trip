package es.pedrazamiguez.splittrip.domain.exception

class AdminRestrictedOperationException(
    cause: Throwable? = null
) : Exception("This operation is restricted to administrators only.", cause)
