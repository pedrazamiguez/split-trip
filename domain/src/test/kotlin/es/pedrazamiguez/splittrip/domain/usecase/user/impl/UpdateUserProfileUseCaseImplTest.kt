package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserProfileUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateUserProfileUseCaseImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userValidationService: UserValidationService
    private lateinit var useCase: UpdateUserProfileUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userValidationService = mockk()
        useCase = UpdateUserProfileUseCaseImpl(
            userRepository = userRepository,
            userValidationService = userValidationService
        )
    }

    @Nested
    inner class Invoke {

        @Test
        fun `returns success when all validations pass and repository succeeds`() = runTest {
            // Given
            val userId = "user-123"
            val displayName = "New Name"
            val bio = "New Bio"
            val localAvatarUri = "file://avatar.webp"

            every { userValidationService.validateDisplayName(displayName) } returns ValidationResult.Valid
            every { userValidationService.validateBio(bio) } returns ValidationResult.Valid
            coEvery { userRepository.updateUserProfile(userId, displayName, bio, localAvatarUri) } returns
                Result.success(Unit)

            // When
            val result = useCase(userId, displayName, bio, localAvatarUri)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { userRepository.updateUserProfile(userId, displayName, bio, localAvatarUri) }
        }

        @Test
        fun `returns failure when display name validation fails`() = runTest {
            // Given
            val userId = "user-123"
            val displayName = ""
            val bio = "New Bio"
            val localAvatarUri = "file://avatar.webp"

            every { userValidationService.validateDisplayName(displayName) } returns
                ValidationResult.Invalid("Display name cannot be empty")
            every { userValidationService.validateBio(bio) } returns ValidationResult.Valid

            // When
            val result = useCase(userId, displayName, bio, localAvatarUri)

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is IllegalArgumentException)
            assertEquals("Display name cannot be empty", exception?.message)
            coVerify(exactly = 0) { userRepository.updateUserProfile(any(), any(), any(), any()) }
        }

        @Test
        fun `returns failure when bio validation fails`() = runTest {
            // Given
            val userId = "user-123"
            val displayName = "Valid Name"
            val bio = "Too long bio..."
            val localAvatarUri = "file://avatar.webp"

            every { userValidationService.validateDisplayName(displayName) } returns ValidationResult.Valid
            every { userValidationService.validateBio(bio) } returns
                ValidationResult.Invalid("Bio cannot exceed 150 characters")

            // When
            val result = useCase(userId, displayName, bio, localAvatarUri)

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is IllegalArgumentException)
            assertEquals("Bio cannot exceed 150 characters", exception?.message)
            coVerify(exactly = 0) { userRepository.updateUserProfile(any(), any(), any(), any()) }
        }

        @Test
        fun `does not validate display name when it is null but validates bio and succeeds`() = runTest {
            // Given
            val userId = "user-123"
            val displayName = null
            val bio = "Valid Bio"
            val localAvatarUri = null

            every { userValidationService.validateBio(bio) } returns ValidationResult.Valid
            coEvery { userRepository.updateUserProfile(userId, displayName, bio, localAvatarUri) } returns
                Result.success(Unit)

            // When
            val result = useCase(userId, displayName, bio, localAvatarUri)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { userValidationService.validateDisplayName(any()) }
            coVerify(exactly = 1) { userRepository.updateUserProfile(userId, displayName, bio, localAvatarUri) }
        }
    }
}
