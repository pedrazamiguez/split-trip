package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.service.DebtSimplificationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetSettlementSuggestionsUseCaseImplTest {

    private val debtSimplificationService = mockk<DebtSimplificationService>()
    private val useCase = GetSettlementSuggestionsUseCaseImpl(debtSimplificationService)

    @Test
    fun `invoke delegates to DebtSimplificationService`() {
        val memberBalances = listOf(
            MemberBalance(userId = "1", pocketBalance = -1000, cashInHand = 0),
            MemberBalance(userId = "2", pocketBalance = 1000, cashInHand = 0)
        )
        val expectedSettlements = listOf(
            Settlement(fromUserId = "1", toUserId = "2", amount = 1000L)
        )

        every { debtSimplificationService.simplify(memberBalances) } returns expectedSettlements

        val result = useCase(memberBalances)

        assertEquals(expectedSettlements, result)
        verify(exactly = 1) { debtSimplificationService.simplify(memberBalances) }
    }
}
