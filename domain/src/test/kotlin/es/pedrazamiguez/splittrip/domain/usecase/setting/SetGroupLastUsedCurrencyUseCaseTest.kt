package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetGroupLastUsedCurrencyUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetGroupLastUsedCurrencyUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk(relaxed = true)
    private val useCase = SetGroupLastUsedCurrencyUseCaseImpl(repository)

    @Test
    fun `delegates to repository with group id and currency code`() = runTest {
        val groupId = "group-123"
        val currencyCode = "EUR"

        useCase(groupId, currencyCode)

        coVerify { repository.setGroupLastUsedCurrency(groupId, currencyCode) }
    }
}
