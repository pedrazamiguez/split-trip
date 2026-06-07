package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetGroupLastUsedCurrencyUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetGroupLastUsedCurrencyUseCaseTest {

    private val repository: GroupPreferenceRepository = mockk()
    private val useCase = GetGroupLastUsedCurrencyUseCaseImpl(repository)

    @Test
    fun `returns currency flow from repository`() = runTest {
        val groupId = "group-123"
        val expectedCurrency = "USD"
        every { repository.getGroupLastUsedCurrency(groupId) } returns flowOf(expectedCurrency)

        val result = useCase(groupId).first()

        assertEquals(expectedCurrency, result)
    }

    @Test
    fun `returns null when no currency stored`() = runTest {
        val groupId = "group-456"
        every { repository.getGroupLastUsedCurrency(groupId) } returns flowOf(null)

        val result = useCase(groupId).first()

        assertNull(result)
    }
}
