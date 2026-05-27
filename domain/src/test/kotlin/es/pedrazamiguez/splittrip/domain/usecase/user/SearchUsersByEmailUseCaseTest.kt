package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SearchUsersByEmailUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: SearchUsersByEmailUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = SearchUsersByEmailUseCase(userRepository)
    }

    @Nested
    inner class Invocation {

        @Test
        fun `returns Result success with user list when found`() = runTest {
            // Given
            val email = "alice@example.com"
            val users = listOf(mockk<User>())
            coEvery { userRepository.searchUsersByEmail(email) } returns users

            // When
            val result = useCase(email)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(users, result.getOrNull())
        }

        @Test
        fun `returns Result success with empty list when no users found`() = runTest {
            // Given
            val email = "unknown@example.com"
            coEvery { userRepository.searchUsersByEmail(email) } returns emptyList()

            // When
            val result = useCase(email)

            // Then
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }

        @Test
        fun `wraps repository exception in Result failure`() = runTest {
            // Given
            val email = "bad@example.com"
            coEvery { userRepository.searchUsersByEmail(email) } throws RuntimeException("Network error")

            // When
            val result = useCase(email)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

        @Test
        fun `delegates to repository with the provided email`() = runTest {
            // Given
            val email = "test@example.com"
            coEvery { userRepository.searchUsersByEmail(any()) } returns emptyList()

            // When
            useCase(email)

            // Then
            coVerify(exactly = 1) { userRepository.searchUsersByEmail(email) }
        }
    }
}
