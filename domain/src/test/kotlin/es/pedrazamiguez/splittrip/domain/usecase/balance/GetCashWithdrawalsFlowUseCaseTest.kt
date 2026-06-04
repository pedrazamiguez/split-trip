package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetCashWithdrawalsFlowUseCaseTest {

    private val repository: CashWithdrawalRepository = mockk()
    private val useCase = GetCashWithdrawalsFlowUseCase(repository)

    @Test
    fun `invoke delegates to repository`() {
        val groupId = "group_123"
        val expectedFlow = flowOf(emptyList<CashWithdrawal>())
        every { repository.getGroupWithdrawalsFlow(groupId) } returns expectedFlow

        val resultFlow = useCase(groupId)

        assertEquals(expectedFlow, resultFlow)
        verify(exactly = 1) { repository.getGroupWithdrawalsFlow(groupId) }
    }
}
