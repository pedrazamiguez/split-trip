package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.PasswordRequirementStatus

interface PasswordValidationService {
    fun validate(password: String): PasswordRequirementStatus
    fun isValidPassword(password: String): Boolean
}
