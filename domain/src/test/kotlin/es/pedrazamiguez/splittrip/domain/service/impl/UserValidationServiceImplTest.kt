package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserValidationServiceImplTest {

    private val service = UserValidationServiceImpl()

    @Test
    fun `constants exposed on companion match expected values`() {
        assertEquals(50, UserValidationService.MAX_DISPLAY_NAME_LENGTH)
        assertEquals(150, UserValidationService.MAX_BIO_LENGTH)
    }

    @Nested
    inner class ValidateDisplayName {

        @Test
        fun `validateDisplayName returns Valid for standard display name`() {
            // When
            val result = service.validateDisplayName("John Doe")

            // Then
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `validateDisplayName returns Invalid for blank name`() {
            // When
            val result = service.validateDisplayName("   ")

            // Then
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("Display name cannot be empty", (result as ValidationResult.Invalid).message)
        }

        @Test
        fun `validateDisplayName returns Invalid for empty name`() {
            // When
            val result = service.validateDisplayName("")

            // Then
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("Display name cannot be empty", (result as ValidationResult.Invalid).message)
        }

        @Test
        fun `validateDisplayName returns Valid for exactly 50 characters`() {
            // Given
            val name = "a".repeat(UserValidationService.MAX_DISPLAY_NAME_LENGTH)

            // When
            val result = service.validateDisplayName(name)

            // Then
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `validateDisplayName returns Invalid for 51 characters`() {
            // Given
            val name = "a".repeat(UserValidationService.MAX_DISPLAY_NAME_LENGTH + 1)

            // When
            val result = service.validateDisplayName(name)

            // Then
            assertTrue(result is ValidationResult.Invalid)
            assertEquals(
                "Display name cannot exceed ${UserValidationService.MAX_DISPLAY_NAME_LENGTH} characters",
                (result as ValidationResult.Invalid).message
            )
        }
    }

    @Nested
    inner class ValidateBio {

        @Test
        fun `validateBio returns Valid for null bio`() {
            // When
            val result = service.validateBio(null)

            // Then
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `validateBio returns Valid for empty bio`() {
            // When
            val result = service.validateBio("")

            // Then
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `validateBio returns Valid for standard bio`() {
            // When
            val result = service.validateBio("Kotlin enthusiast, traveller")

            // Then
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `validateBio returns Valid for exactly 150 characters`() {
            // Given
            val bio = "b".repeat(UserValidationService.MAX_BIO_LENGTH)

            // When
            val result = service.validateBio(bio)

            // Then
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `validateBio returns Invalid for 151 characters`() {
            // Given
            val bio = "b".repeat(UserValidationService.MAX_BIO_LENGTH + 1)

            // When
            val result = service.validateBio(bio)

            // Then
            assertTrue(result is ValidationResult.Invalid)
            assertEquals(
                "Bio cannot exceed ${UserValidationService.MAX_BIO_LENGTH} characters",
                (result as ValidationResult.Invalid).message
            )
        }
    }
}
