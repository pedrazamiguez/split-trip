package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.service.impl.PasswordValidationServiceImpl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PasswordValidationServiceImpl")
class PasswordValidationServiceImplTest {

    private val service = PasswordValidationServiceImpl()

    @Nested
    @DisplayName("Valid passwords")
    inner class ValidPasswords {

        @Test
        fun `valid complex password meets all requirements`() {
            val status = service.validate("P@ssword1")
            assertTrue(status.isMinLengthValid)
            assertTrue(status.hasUpperCase)
            assertTrue(status.hasLowerCase)
            assertTrue(status.hasDigit)
            assertTrue(status.hasSpecialChar)
            assertTrue(status.isValid)
            assertTrue(service.isValidPassword("P@ssword1"))
        }

        @Test
        fun `password with different special characters is valid`() {
            val validPasswords = listOf(
                "Secure#2026",
                "Split-Trip!1",
                "Pass_word9$",
                "Valid.Pass8?",
                "Complex+Key1%",
                "Symbols=Great1&",
                "Euro€Symbol9A"
            )

            validPasswords.forEach { password ->
                val status = service.validate(password)
                assertTrue(status.isValid, "Expected $password to be valid")
                assertTrue(service.isValidPassword(password), "Expected isValidPassword for $password to be true")
            }
        }

        @Test
        fun `long complex password is valid`() {
            val status = service.validate("VeryLongAndSecurePassword123!@#")
            assertTrue(status.isValid)
            assertTrue(service.isValidPassword("VeryLongAndSecurePassword123!@#"))
        }
    }

    @Nested
    @DisplayName("Length requirement")
    inner class LengthRequirement {

        @Test
        fun `password with 7 characters is invalid for length`() {
            val status = service.validate("P@ssw1A")
            assertFalse(status.isMinLengthValid)
            assertTrue(status.hasUpperCase)
            assertTrue(status.hasLowerCase)
            assertTrue(status.hasDigit)
            assertTrue(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword("P@ssw1A"))
        }

        @Test
        fun `password with 8 characters meets length requirement`() {
            val status = service.validate("P@ssw1Ab")
            assertTrue(status.isMinLengthValid)
        }
    }

    @Nested
    @DisplayName("Uppercase requirement")
    inner class UppercaseRequirement {

        @Test
        fun `password without uppercase is invalid`() {
            val status = service.validate("p@ssword1")
            assertTrue(status.isMinLengthValid)
            assertFalse(status.hasUpperCase)
            assertTrue(status.hasLowerCase)
            assertTrue(status.hasDigit)
            assertTrue(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword("p@ssword1"))
        }
    }

    @Nested
    @DisplayName("Lowercase requirement")
    inner class LowercaseRequirement {

        @Test
        fun `password without lowercase is invalid`() {
            val status = service.validate("P@SSWORD1")
            assertTrue(status.isMinLengthValid)
            assertTrue(status.hasUpperCase)
            assertFalse(status.hasLowerCase)
            assertTrue(status.hasDigit)
            assertTrue(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword("P@SSWORD1"))
        }
    }

    @Nested
    @DisplayName("Digit requirement")
    inner class DigitRequirement {

        @Test
        fun `password without digit is invalid`() {
            val status = service.validate("P@ssword!")
            assertTrue(status.isMinLengthValid)
            assertTrue(status.hasUpperCase)
            assertTrue(status.hasLowerCase)
            assertFalse(status.hasDigit)
            assertTrue(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword("P@ssword!"))
        }
    }

    @Nested
    @DisplayName("Special character requirement")
    inner class SpecialCharacterRequirement {

        @Test
        fun `password without special character is invalid`() {
            val status = service.validate("Password123")
            assertTrue(status.isMinLengthValid)
            assertTrue(status.hasUpperCase)
            assertTrue(status.hasLowerCase)
            assertTrue(status.hasDigit)
            assertFalse(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword("Password123"))
        }

        @Test
        fun `space is not counted as special character`() {
            val status = service.validate("Password 1")
            assertTrue(status.isMinLengthValid)
            assertTrue(status.hasUpperCase)
            assertTrue(status.hasLowerCase)
            assertTrue(status.hasDigit)
            assertFalse(status.hasSpecialChar)
            assertFalse(status.isValid)
        }
    }

    @Nested
    @DisplayName("Empty and blank inputs")
    inner class EmptyAndBlankInputs {

        @Test
        fun `empty string fails all requirements`() {
            val status = service.validate("")
            assertFalse(status.isMinLengthValid)
            assertFalse(status.hasUpperCase)
            assertFalse(status.hasLowerCase)
            assertFalse(status.hasDigit)
            assertFalse(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword(""))
        }

        @Test
        fun `blank string fails all requirements except min length if 8 spaces`() {
            val status = service.validate("        ")
            assertTrue(status.isMinLengthValid)
            assertFalse(status.hasUpperCase)
            assertFalse(status.hasLowerCase)
            assertFalse(status.hasDigit)
            assertFalse(status.hasSpecialChar)
            assertFalse(status.isValid)
            assertFalse(service.isValidPassword("        "))
        }
    }
}
