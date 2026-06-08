package es.pedrazamiguez.splittrip.domain.service

interface EmailValidationService {
    fun isValidEmail(email: String): Boolean
}
