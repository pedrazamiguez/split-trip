package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.service.EmailValidationService

class EmailValidationServiceImpl : EmailValidationService {

    override fun isValidEmail(email: String): Boolean = email.trim().matches(EMAIL_REGEX)

    companion object {
        private val EMAIL_REGEX = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
        )
    }
}
