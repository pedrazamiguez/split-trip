package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.LinkEmailPasswordUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LinkEmailPasswordUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var userRepository: UserRepository
    private lateinit var reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
    private lateinit var useCase: LinkEmailPasswordUseCase

    private val email = "email@test.com"
    private val password = "pass"
    private val userId = "userId-123"

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        userRepository = mockk()
        reconcileUnregisteredUserUseCase = mockk()
        useCase = LinkEmailPasswordUseCaseImpl(
            authenticationService = authenticationService,
            userRepository = userRepository,
            reconcileUnregisteredUserUseCase = reconcileUnregisteredUserUseCase
        )

        coEvery { authenticationService.requireUserId() } returns userId
        coEvery { userRepository.getCurrentUserProfile() } returns User(userId, "", "user", null, null)
        coEvery { userRepository.saveUser(any()) } returns Result.success(Unit)
        coEvery { reconcileUnregisteredUserUseCase(any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `invokes linkEmailPassword on service and reconciles user`() = runTest {
        coEvery { authenticationService.linkEmailPassword(email, password) } returns Result.success(Unit)

        val result = useCase(email, password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authenticationService.linkEmailPassword(email, password) }
        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { reconcileUnregisteredUserUseCase(email, userId) }
    }

    @Test
    fun `returns failure when service fails`() = runTest {
        val exception = RuntimeException("Link failed")
        coEvery { authenticationService.linkEmailPassword(email, password) } returns Result.failure(exception)

        val result = useCase(email, password)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 0) { reconcileUnregisteredUserUseCase(any(), any()) }
    }

    @Test
    fun `succeeds even when reconciliation fails`() = runTest {
        coEvery { authenticationService.linkEmailPassword(email, password) } returns Result.success(Unit)
        coEvery { reconcileUnregisteredUserUseCase(any(), any()) } returns
            Result.failure(RuntimeException("Reconciliation failed"))

        val result = useCase(email, password)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authenticationService.linkEmailPassword(email, password) }
        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { reconcileUnregisteredUserUseCase(email, userId) }
    }
}
