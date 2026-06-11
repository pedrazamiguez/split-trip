package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.impl.GetLinkedProvidersUseCaseImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetLinkedProvidersUseCaseTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var useCase: GetLinkedProvidersUseCase

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        useCase = GetLinkedProvidersUseCaseImpl(authenticationService)
    }

    @Test
    fun `invokes getLinkedProviders on service`() = runTest {
        val list = listOf(AuthProviderType.EMAIL_PASSWORD)
        coEvery { authenticationService.getLinkedProviders() } returns Result.success(list)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(list, result.getOrNull())
    }

    @Test
    fun `returns failure when service fails`() = runTest {
        val exception = RuntimeException("Get providers failed")
        coEvery { authenticationService.getLinkedProviders() } returns Result.failure(exception)

        val result = useCase()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
