package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.LinkGoogleAccountUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LinkGoogleAccountUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var useCase: LinkGoogleAccountUseCase

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        useCase = LinkGoogleAccountUseCaseImpl(authenticationService)
    }

    @Test
    fun `invokes linkGoogleAccount on service`() = runTest {
        coEvery { authenticationService.linkGoogleAccount("id-token") } returns Result.success(Unit)

        val result = useCase("id-token")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when service fails`() = runTest {
        val exception = RuntimeException("Link failed")
        coEvery { authenticationService.linkGoogleAccount("id-token") } returns Result.failure(exception)

        val result = useCase("id-token")

        assertTrue(result.isFailure)
        org.junit.jupiter.api.Assertions.assertEquals(exception, result.exceptionOrNull())
    }
}
