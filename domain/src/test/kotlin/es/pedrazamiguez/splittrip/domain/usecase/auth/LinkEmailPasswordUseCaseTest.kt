package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.LinkEmailPasswordUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LinkEmailPasswordUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var useCase: LinkEmailPasswordUseCase

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        useCase = LinkEmailPasswordUseCaseImpl(authenticationService)
    }

    @Test
    fun `invokes linkEmailPassword on service`() = runTest {
        coEvery { authenticationService.linkEmailPassword("email@test.com", "pass") } returns Result.success(Unit)

        val result = useCase("email@test.com", "pass")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when service fails`() = runTest {
        val exception = RuntimeException("Link failed")
        coEvery { authenticationService.linkEmailPassword("email@test.com", "pass") } returns Result.failure(exception)

        val result = useCase("email@test.com", "pass")

        assertTrue(result.isFailure)
        org.junit.jupiter.api.Assertions.assertEquals(exception, result.exceptionOrNull())
    }
}
