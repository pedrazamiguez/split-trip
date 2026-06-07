package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetSelectedGroupCurrencyUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetSelectedGroupCurrencyUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk()
    private val useCase = GetSelectedGroupCurrencyUseCaseImpl(repository)

    @Test
    fun `returns currency flow from repository`() = runTest {
        val expectedCurrency = "EUR"
        every { repository.getSelectedGroupCurrency() } returns flowOf(expectedCurrency)

        val result = useCase().first()

        assertEquals(expectedCurrency, result)
    }

    @Test
    fun `returns null when no currency stored`() = runTest {
        every { repository.getSelectedGroupCurrency() } returns flowOf(null)

        val result = useCase().first()

        assertNull(result)
    }
}
