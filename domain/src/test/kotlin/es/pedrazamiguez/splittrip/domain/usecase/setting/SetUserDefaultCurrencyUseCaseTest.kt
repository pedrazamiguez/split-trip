package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetUserDefaultCurrencyUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetUserDefaultCurrencyUseCaseTest {

    private val repository: UserPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetUserDefaultCurrencyUseCaseImpl(repository)

    @Test
    fun `delegates to repository with currency code`() = runTest {
        val currencyCode = "EUR"

        useCase(currencyCode)

        coVerify { repository.setUserDefaultCurrency(currencyCode) }
    }

    @Test
    fun `delegates to repository when switching currency`() = runTest {
        val currencyCode = "USD"

        useCase(currencyCode)

        coVerify { repository.setUserDefaultCurrency(currencyCode) }
    }
}
