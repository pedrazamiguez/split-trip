package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EmailValidationService")
class EmailValidationServiceTest {

    private val service = EmailValidationServiceImpl()

    @Nested
    @DisplayName("Valid emails")
    inner class ValidEmails {

        @Test
        fun `simple email is valid`() {
            assertTrue(service.isValidEmail("user@example.com"))
        }

        @Test
        fun `email with subdomain is valid`() {
            assertTrue(service.isValidEmail("user@mail.example.com"))
        }

        @Test
        fun `email with plus sign is valid`() {
            assertTrue(service.isValidEmail("user+tag@example.com"))
        }

        @Test
        fun `email with dots in local part is valid`() {
            assertTrue(service.isValidEmail("first.last@example.com"))
        }

        @Test
        fun `email with hyphens in domain is valid`() {
            assertTrue(service.isValidEmail("user@my-domain.com"))
        }

        @Test
        fun `email with numbers is valid`() {
            assertTrue(service.isValidEmail("user123@example456.com"))
        }

        @Test
        fun `email with leading or trailing spaces is valid after trim`() {
            assertTrue(service.isValidEmail("  user@example.com  "))
        }
    }

    @Nested
    @DisplayName("Invalid emails")
    inner class InvalidEmails {

        @Test
        fun `empty string is invalid`() {
            assertFalse(service.isValidEmail(""))
        }

        @Test
        fun `blank string is invalid`() {
            assertFalse(service.isValidEmail("   "))
        }

        @Test
        fun `missing at sign is invalid`() {
            assertFalse(service.isValidEmail("userexample.com"))
        }

        @Test
        fun `missing domain is invalid`() {
            assertFalse(service.isValidEmail("user@"))
        }

        @Test
        fun `missing local part is invalid`() {
            assertFalse(service.isValidEmail("@example.com"))
        }

        @Test
        fun `missing TLD is invalid`() {
            assertFalse(service.isValidEmail("user@example"))
        }

        @Test
        fun `partial email is invalid`() {
            assertFalse(service.isValidEmail("john@gma"))
        }

        @Test
        fun `just a name is invalid`() {
            assertFalse(service.isValidEmail("john"))
        }

        @Test
        fun `double at sign is invalid`() {
            assertFalse(service.isValidEmail("user@@example.com"))
        }
    }
}
