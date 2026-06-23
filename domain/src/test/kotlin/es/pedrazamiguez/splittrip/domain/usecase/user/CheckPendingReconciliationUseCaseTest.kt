package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.CheckPendingReconciliationUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CheckPendingReconciliationUseCaseTest {

    private lateinit var userRepository: UserRepository
    private lateinit var useCase: CheckPendingReconciliationUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = CheckPendingReconciliationUseCaseImpl(
            userRepository = userRepository
        )
    }

    @Nested
    inner class Invoke {

        @Test
        fun `returns true when pending user profile is found`() = runTest {
            // Given
            val email = "pending@example.com"
            val pendingUserId = User.generatePendingUserId(email)
            val mockUser: User = mockk()
            coEvery { userRepository.getUsersByIds(listOf(pendingUserId)) } returns mapOf(pendingUserId to mockUser)

            // When
            val result = useCase(email)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
        }

        @Test
        fun `returns false when no pending user profile exists`() = runTest {
            // Given
            val email = "no_pending@example.com"
            val pendingUserId = User.generatePendingUserId(email)
            coEvery { userRepository.getUsersByIds(listOf(pendingUserId)) } returns emptyMap()

            // When
            val result = useCase(email)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

        @Test
        fun `returns failure when remote repository check fails`() = runTest {
            // Given
            val email = "error@example.com"
            val pendingUserId = User.generatePendingUserId(email)
            val exception = RuntimeException("Firestore lookup failed")
            coEvery { userRepository.getUsersByIds(listOf(pendingUserId)) } throws exception

            // When
            val result = useCase(email)

            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
    }
}
