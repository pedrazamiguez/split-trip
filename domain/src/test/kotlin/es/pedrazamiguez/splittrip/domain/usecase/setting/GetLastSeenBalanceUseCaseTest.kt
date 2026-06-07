package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.GetLastSeenBalanceUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GetLastSeenBalanceUseCaseTest {

    private val repository: BalancePreferenceRepository = mockk()
    private val useCase = GetLastSeenBalanceUseCaseImpl(repository)

    @Test
    fun `returns balance flow from repository`() = runTest {
        val groupId = "group-123"
        val expectedBalance = "€42.50"
        every { repository.getLastSeenBalance(groupId) } returns flowOf(expectedBalance)

        val result = useCase(groupId).first()

        assertEquals(expectedBalance, result)
    }

    @Test
    fun `returns null when no balance stored`() = runTest {
        val groupId = "group-456"
        every { repository.getLastSeenBalance(groupId) } returns flowOf(null)

        val result = useCase(groupId).first()

        assertNull(result)
    }
}
