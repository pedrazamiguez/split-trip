package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GetCashWithdrawalsByIdsUseCaseImpl")
class GetCashWithdrawalsByIdsUseCaseImplTest {

    private val cashWithdrawalRepository = mockk<CashWithdrawalRepository>()
    private lateinit var useCase: GetCashWithdrawalsByIdsUseCaseImpl

    @BeforeEach
    fun setUp() {
        useCase = GetCashWithdrawalsByIdsUseCaseImpl(cashWithdrawalRepository)
    }

    @Test
    fun `invoke with empty withdrawalIds returns empty list without calling repository`() = runTest {
        val result = useCase(emptyList())

        assertEquals(0, result.size)
        coVerify(exactly = 0) { cashWithdrawalRepository.getWithdrawalsByIds(any()) }
    }

    @Test
    fun `invoke with withdrawalIds delegates to repository`() = runTest {
        val ids = listOf("with-1", "with-2")
        val withdrawals = listOf(
            CashWithdrawal(id = "with-1", groupId = "group-1"),
            CashWithdrawal(id = "with-2", groupId = "group-1")
        )
        coEvery { cashWithdrawalRepository.getWithdrawalsByIds(ids) } returns withdrawals

        val result = useCase(ids)

        assertEquals(2, result.size)
        assertEquals(withdrawals, result)
        coVerify(exactly = 1) { cashWithdrawalRepository.getWithdrawalsByIds(ids) }
    }
}
