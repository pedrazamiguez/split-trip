package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.CheckPendingReconciliationUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
            assertTrue(result.exceptionOrNull() is RuntimeException)
            assertEquals(exception.message, result.exceptionOrNull()?.message)
        }

        @Test
        fun `returns failure with TimeoutCancellationException when repository check exceeds timeout`() = runTest {
            // Given
            val email = "timeout@example.com"
            val pendingUserId = User.generatePendingUserId(email)
            val timedUseCase = CheckPendingReconciliationUseCaseImpl(
                userRepository = userRepository,
                timeoutMs = 1_000L
            )
            coEvery { userRepository.getUsersByIds(listOf(pendingUserId)) } coAnswers {
                delay(10_000L)
                emptyMap()
            }

            // When
            val result = timedUseCase(email)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
        }

        @Test
        fun `rethrows CancellationException when caller coroutine is cancelled`() = runTest {
            // Given
            val email = "cancelled@example.com"
            val pendingUserId = User.generatePendingUserId(email)
            val started = CompletableDeferred<Unit>()
            coEvery { userRepository.getUsersByIds(listOf(pendingUserId)) } coAnswers {
                started.complete(Unit)
                awaitCancellation()
            }
            var returnedResult: Result<Boolean>? = null

            // When
            val job = launch {
                returnedResult = useCase(email)
            }
            started.await()
            job.cancelAndJoin()

            // Then
            assertTrue(job.isCancelled)
            assertNull(returnedResult)
        }
    }
}
