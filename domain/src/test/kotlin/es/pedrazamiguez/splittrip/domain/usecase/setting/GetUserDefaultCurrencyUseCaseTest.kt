package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetUserDefaultCurrencyUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetUserDefaultCurrencyUseCaseTest {

    private val repository: UserPreferenceRepository = mockk()
    private val useCase = GetUserDefaultCurrencyUseCaseImpl(repository)

    @Test
    fun `returns default currency flow from repository`() = runTest {
        val expectedCurrency = "EUR"
        every { repository.getUserDefaultCurrency() } returns flowOf(expectedCurrency)

        val result = useCase().first()

        assertEquals(expectedCurrency, result)
    }

    @Test
    fun `returns updated currency when preference changes`() = runTest {
        val updatedCurrency = "USD"
        every { repository.getUserDefaultCurrency() } returns flowOf(updatedCurrency)

        val result = useCase().first()

        assertEquals(updatedCurrency, result)
    }
}
