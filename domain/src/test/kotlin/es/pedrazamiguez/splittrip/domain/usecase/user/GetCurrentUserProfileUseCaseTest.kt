package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.GetCurrentUserProfileUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetCurrentUserProfileUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: GetCurrentUserProfileUseCase

    private val testUser = User(
        userId = "user-123",
        email = "test@example.com",
        displayName = "Test User",
        profileImagePath = "https://example.com/photo.jpg",
        createdAt = LocalDateTime.of(2024, 6, 15, 10, 30)
    )

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = GetCurrentUserProfileUseCaseImpl(userRepository = userRepository)
    }

    @Nested
    inner class Invoke {

        @Test
        fun `returns user profile from repository`() = runTest {
            // Given
            coEvery { userRepository.getCurrentUserProfile() } returns testUser

            // When
            val result = useCase()

            // Then
            assertEquals(testUser, result)
            coVerify(exactly = 1) { userRepository.getCurrentUserProfile() }
        }

        @Test
        fun `returns null when repository returns null`() = runTest {
            // Given
            coEvery { userRepository.getCurrentUserProfile() } returns null

            // When
            val result = useCase()

            // Then
            assertNull(result)
            coVerify(exactly = 1) { userRepository.getCurrentUserProfile() }
        }
    }
}
