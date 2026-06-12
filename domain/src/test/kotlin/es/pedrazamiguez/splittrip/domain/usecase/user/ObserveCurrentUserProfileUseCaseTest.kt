package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.ObserveCurrentUserProfileUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ObserveCurrentUserProfileUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: ObserveCurrentUserProfileUseCase

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
        useCase = ObserveCurrentUserProfileUseCaseImpl(userRepository = userRepository)
    }

    @Nested
    inner class Invoke {

        @Test
        fun `returns flow of user profile from repository`() = runTest {
            // Given
            val flow = flowOf(testUser, null)
            every { userRepository.observeCurrentUserProfile() } returns flow

            // When
            val result = useCase().toList()

            // Then
            assertEquals(2, result.size)
            assertEquals(testUser, result[0])
            assertEquals(null, result[1])
            verify(exactly = 1) { userRepository.observeCurrentUserProfile() }
        }
    }
}
