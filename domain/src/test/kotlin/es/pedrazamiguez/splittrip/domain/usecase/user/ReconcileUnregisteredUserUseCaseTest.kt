package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.ReconcileUnregisteredUserUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ReconcileUnregisteredUserUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var userRepository: UserRepository
    private lateinit var useCase: ReconcileUnregisteredUserUseCase

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        userRepository = mockk()
        useCase = ReconcileUnregisteredUserUseCaseImpl(
            groupRepository = groupRepository,
            userRepository = userRepository
        )
    }

    @Nested
    inner class Invoke {

        @Test
        fun `reconciles user and deletes pending user when successful`() = runTest {
            // Given
            val email = "pending@example.com"
            val activeUserId = "active-user-123"
            val pendingUserId = User.generatePendingUserId(email)

            coEvery { groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId) } returns Unit
            coEvery { userRepository.deletePendingUser(pendingUserId) } returns Result.success(Unit)

            // When
            val result = useCase(email, activeUserId)

            // Then
            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId) }
            coVerify(exactly = 1) { userRepository.deletePendingUser(pendingUserId) }
        }

        @Test
        fun `returns failure when group reconciliation fails`() = runTest {
            // Given
            val email = "pending@example.com"
            val activeUserId = "active-user-123"
            val pendingUserId = User.generatePendingUserId(email)

            val exception = RuntimeException("Firestore error")
            coEvery { groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId) } throws exception

            // When
            val result = useCase(email, activeUserId)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId) }
            coVerify(exactly = 0) { userRepository.deletePendingUser(any()) }
        }

        @Test
        fun `returns failure when deleting pending user fails`() = runTest {
            // Given
            val email = "pending@example.com"
            val activeUserId = "active-user-123"
            val pendingUserId = User.generatePendingUserId(email)

            val exception = RuntimeException("DB delete error")
            coEvery { groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId) } returns Unit
            coEvery { userRepository.deletePendingUser(pendingUserId) } returns Result.failure(exception)

            // When
            val result = useCase(email, activeUserId)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 1) { groupRepository.reconcileUnregisteredUser(pendingUserId, activeUserId) }
            coVerify(exactly = 1) { userRepository.deletePendingUser(pendingUserId) }
        }
    }
}
