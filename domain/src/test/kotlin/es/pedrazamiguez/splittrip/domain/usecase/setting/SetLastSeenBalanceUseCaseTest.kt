package es.pedrazamiguez.splittrip.domain.usecase.setting

import es.pedrazamiguez.splittrip.domain.repository.BalancePreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.setting.impl.SetLastSeenBalanceUseCaseImpl
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SetLastSeenBalanceUseCaseTest {

    private val repository: BalancePreferenceRepository = mockk(relaxed = true)
    private val useCase = SetLastSeenBalanceUseCaseImpl(repository)

    @Test
    fun `delegates to repository with correct parameters`() = runTest {
        val groupId = "group-123"
        val formattedBalance = "€42.50"

        useCase(groupId, formattedBalance)

        coVerify { repository.setLastSeenBalance(groupId, formattedBalance) }
    }

    @Test
    fun `handles empty balance string`() = runTest {
        val groupId = "group-456"
        val formattedBalance = ""

        useCase(groupId, formattedBalance)

        coVerify { repository.setLastSeenBalance(groupId, formattedBalance) }
    }
}
