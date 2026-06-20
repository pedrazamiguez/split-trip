package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.LinkGoogleAccountUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LinkGoogleAccountUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var userRepository: UserRepository
    private lateinit var reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
    private lateinit var useCase: LinkGoogleAccountUseCase

    private val idToken = "id-token"
    private val email = "google@test.com"
    private val userId = "userId-123"

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        userRepository = mockk()
        reconcileUnregisteredUserUseCase = mockk()
        useCase = LinkGoogleAccountUseCaseImpl(
            authenticationService = authenticationService,
            userRepository = userRepository,
            reconcileUnregisteredUserUseCase = reconcileUnregisteredUserUseCase
        )

        coEvery { authenticationService.currentUserEmail() } returns email
        coEvery { authenticationService.requireUserId() } returns userId
        coEvery { userRepository.getCurrentUserProfile() } returns User(userId, "", "user", null, null)
        coEvery { userRepository.saveUser(any()) } returns Result.success(Unit)
        coEvery { reconcileUnregisteredUserUseCase(any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `invokes linkGoogleAccount on service and reconciles user`() = runTest {
        coEvery { authenticationService.linkGoogleAccount(idToken) } returns Result.success(Unit)

        val result = useCase(idToken)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { authenticationService.linkGoogleAccount(idToken) }
        coVerify(exactly = 1) { userRepository.saveUser(any()) }
        coVerify(exactly = 1) { reconcileUnregisteredUserUseCase(email, userId) }
    }

    @Test
    fun `returns failure when service fails`() = runTest {
        val exception = RuntimeException("Link failed")
        coEvery { authenticationService.linkGoogleAccount(idToken) } returns Result.failure(exception)

        val result = useCase(idToken)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 0) { reconcileUnregisteredUserUseCase(any(), any()) }
    }
}
