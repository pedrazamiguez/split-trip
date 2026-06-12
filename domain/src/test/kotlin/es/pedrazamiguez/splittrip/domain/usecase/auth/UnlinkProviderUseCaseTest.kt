package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.UnlinkProviderUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UnlinkProviderUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var useCase: UnlinkProviderUseCase

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        useCase = UnlinkProviderUseCaseImpl(authenticationService)
    }

    @Test
    fun `invokes unlinkProvider on service`() = runTest {
        coEvery { authenticationService.unlinkProvider(AuthProviderType.GOOGLE) } returns Result.success(Unit)

        val result = useCase(AuthProviderType.GOOGLE)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when service fails`() = runTest {
        val exception = RuntimeException("Unlink failed")
        coEvery { authenticationService.unlinkProvider(AuthProviderType.GOOGLE) } returns Result.failure(exception)

        val result = useCase(AuthProviderType.GOOGLE)

        assertTrue(result.isFailure)
        org.junit.jupiter.api.Assertions.assertEquals(exception, result.exceptionOrNull())
    }
}
