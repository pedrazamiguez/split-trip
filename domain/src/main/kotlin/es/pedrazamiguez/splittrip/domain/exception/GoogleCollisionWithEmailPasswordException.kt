package es.pedrazamiguez.splittrip.domain.exception

class GoogleCollisionWithEmailPasswordException(
    val email: String,
    val idToken: String,
    cause: Throwable? = null
) : Exception("Google sign-in collided with existing email/password account for $email", cause)
