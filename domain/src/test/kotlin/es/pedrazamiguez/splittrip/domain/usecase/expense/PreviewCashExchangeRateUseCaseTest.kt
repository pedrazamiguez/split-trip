package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.PreviewCashExchangeRateUseCaseImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PreviewCashExchangeRateUseCaseTest {

    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var expenseCalculatorService: ExpenseCalculatorService
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var useCase: PreviewCashExchangeRateUseCase

    private val groupId = "group-123"
    private val currency = "THB"

    private val withdrawal1 = CashWithdrawal(
        id = "w-1",
        groupId = groupId,
        amountWithdrawn = 1000000L, // 10,000 THB
        remainingAmount = 1000000L,
        currency = currency,
        deductedBaseAmount = 27000L, // 270.00 EUR → rate ~37.037
        exchangeRate = BigDecimal("37.037037"),
        createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
    )

    private val withdrawal2 = CashWithdrawal(
        id = "w-2",
        groupId = groupId,
        amountWithdrawn = 500000L, // 5,000 THB
        remainingAmount = 500000L,
        currency = currency,
        deductedBaseAmount = 13700L, // 137.00 EUR → rate ~36.496
        exchangeRate = BigDecimal("36.496350"),
        createdAt = LocalDateTime.of(2026, 1, 12, 12, 0)
    )

    @BeforeEach
    fun setUp() {
        cashWithdrawalRepository = mockk()
        expenseCalculatorService = ExpenseCalculatorServiceImpl() // Use real service for integration-style tests
        exchangeRateCalculationService = ExchangeRateCalculationServiceImpl()
        useCase = PreviewCashExchangeRateUseCaseImpl(
            cashWithdrawalRepository,
            expenseCalculatorService,
            exchangeRateCalculationService
        )
    }

    // ── No withdrawals ────────────────────────────────────────────────────────

    @Nested
    inner class NoWithdrawals {

        @Test
        fun `returns NoWithdrawals when no withdrawals exist for currency`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns emptyList()

            val result = useCase(groupId, currency, 50000L)

            assertEquals(CashRatePreviewResult.NoWithdrawals, result)
        }

        @Test
        fun `returns NoWithdrawals with zero source amount and no withdrawals`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns emptyList()

            val result = useCase(groupId, currency, 0L)

            assertEquals(CashRatePreviewResult.NoWithdrawals, result)
        }
    }

    // ── Zero source amount (weighted average preview) ─────────────────────────

    @Nested
    inner class WeightedAveragePreview {

        @Test
        fun `returns weighted average display rate when source amount is zero`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1, withdrawal2)

            val result = useCase(groupId, currency, 0L)

            assertTrue(result is CashRatePreviewResult.Available)
            val preview = (result as CashRatePreviewResult.Available).preview
            // Weighted average: (1000000 + 500000) / (27000 + 13700) = 1500000 / 40700 ≈ 36.855037
            assertEquals(BigDecimal("36.855037"), preview.displayRate)
            assertEquals(0L, preview.groupAmountCents) // No FIFO simulation
        }

        @Test
        fun `returns stored exchange rate for single withdrawal instead of computed average`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            val result = useCase(groupId, currency, 0L)

            assertTrue(result is CashRatePreviewResult.Available)
            val preview = (result as CashRatePreviewResult.Available).preview
            // Single withdrawal: uses stored exchangeRate directly (not computed from cents)
            assertEquals(BigDecimal("37.037037"), preview.displayRate)
        }

        @Test
        fun `treats negative source amount as zero and returns preview`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            val result = useCase(groupId, currency, -100L)

            // Negative is treated like zero → weighted average preview
            assertTrue(result is CashRatePreviewResult.Available)
        }
    }

    // ── FIFO-simulated preview ────────────────────────────────────────────────

    @Nested
    inner class FifoSimulatedPreview {

        @Test
        fun `returns stored exchange rate for single-tranche expense`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            // 500 THB (50000 cents) from withdrawal1
            val result = useCase(groupId, currency, 50000L)

            assertTrue(result is CashRatePreviewResult.Available)
            val preview = (result as CashRatePreviewResult.Available).preview
            // FIFO uses withdrawal1's rate: deductedBaseAmount / amountWithdrawn = 27000/1000000
            // groupAmount = 50000 * (27000/1000000) = 50000 * 0.027 = 1350
            assertEquals(1350L, preview.groupAmountCents)
            // Single tranche: uses stored exchangeRate (37.037037) instead of 50000/1350 = 37.037037
            assertEquals(BigDecimal("37.037037"), preview.displayRate)
        }

        @Test
        fun `single-tranche rate stays stable regardless of amount within tranche`() = runTest {
            // Withdrawal with a "clean" rate: 5000 THB at exactly 37.000 (135.14 EUR)
            val cleanWithdrawal = CashWithdrawal(
                id = "w-clean",
                groupId = groupId,
                amountWithdrawn = 500000L, // 5,000 THB
                remainingAmount = 500000L,
                currency = currency,
                deductedBaseAmount = 13514L, // 135.14 EUR
                exchangeRate = BigDecimal("37.000000"),
                createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
            )

            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(cleanWithdrawal)

            // 100 THB — without fix would compute 37.037037 from rounded cents
            val result100 = useCase(groupId, currency, 10000L)
            assertTrue(result100 is CashRatePreviewResult.Available)
            assertEquals(
                BigDecimal("37.000000"),
                (result100 as CashRatePreviewResult.Available).preview.displayRate
            )

            // 4750 THB — without fix would compute 36.999533 from rounded cents
            val result4750 = useCase(groupId, currency, 475000L)
            assertTrue(result4750 is CashRatePreviewResult.Available)
            assertEquals(
                BigDecimal("37.000000"),
                (result4750 as CashRatePreviewResult.Available).preview.displayRate
            )
        }

        @Test
        fun `returns FIFO-blended rate for multi-tranche expense`() = runTest {
            // First withdrawal has only 200 THB remaining
            val partialW1 = withdrawal1.copy(remainingAmount = 20000L)

            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(partialW1, withdrawal2)

            // 500 THB (50000 cents): 200 THB from w1 (rate ~37.037) + 300 THB from w2 (rate ~36.496)
            val result = useCase(groupId, currency, 50000L)

            assertTrue(result is CashRatePreviewResult.Available)
            val preview = (result as CashRatePreviewResult.Available).preview
            // w1: 20000 * (27000/1000000) = 20000 * 0.027 = 540
            // w2: 30000 * (13700/500000)  = 30000 * 0.0274 = 822
            // total = 540 + 822 = 1362
            assertEquals(1362L, preview.groupAmountCents)
            assertTrue(preview.displayRate > BigDecimal.ONE)
        }

        @Test
        fun `returns InsufficientCash when amount exceeds available cash`() = runTest {
            // Only 10,000 THB available
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            // Request 20,000 THB (2000000 cents) — exceeds available
            val result = useCase(groupId, currency, 2000000L)

            assertEquals(CashRatePreviewResult.InsufficientCash, result)
        }
    }

    // ── Tranche population ────────────────────────────────────────────────────

    @Nested
    inner class TranchePopulation {

        @Test
        fun `single-tranche preview populates CashTranchePreview with correct fields`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            val result = useCase(groupId, currency, 50000L) // 500 THB

            assertTrue(result is CashRatePreviewResult.Available)
            val preview = (result as CashRatePreviewResult.Available).preview
            assertEquals(1, preview.tranches.size)
            val tranche = preview.tranches.first()
            assertEquals("w-1", tranche.withdrawalId)
            assertNull(tranche.withdrawalTitle) // withdrawal1 has no title
            assertEquals(LocalDateTime.of(2026, 1, 10, 12, 0), tranche.withdrawalDate)
            assertEquals(50000L, tranche.amountConsumedCents)
            // remainingAfterCents = 1000000 - 50000 = 950000
            assertEquals(950000L, tranche.remainingAfterCents)
            assertEquals(BigDecimal("37.037037"), tranche.withdrawalRate)
        }

        @Test
        fun `single-tranche preview remainingAfterCents is zero when withdrawal fully consumed`() = runTest {
            val exhaustedWithdrawal = withdrawal1.copy(remainingAmount = 50000L)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(exhaustedWithdrawal)

            val result = useCase(groupId, currency, 50000L)

            assertTrue(result is CashRatePreviewResult.Available)
            val tranche = (result as CashRatePreviewResult.Available).preview.tranches.first()
            assertEquals(0L, tranche.remainingAfterCents)
        }

        @Test
        fun `multi-tranche preview populates all tranches with correct remainingAfterCents`() = runTest {
            val partialW1 = withdrawal1.copy(remainingAmount = 20000L) // 200 THB remaining
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(partialW1, withdrawal2)

            // 500 THB: 200 from w1, 300 from w2
            val result = useCase(groupId, currency, 50000L)

            assertTrue(result is CashRatePreviewResult.Available)
            val tranches = (result as CashRatePreviewResult.Available).preview.tranches
            assertEquals(2, tranches.size)

            val t1 = tranches[0]
            assertEquals("w-1", t1.withdrawalId)
            assertEquals(20000L, t1.amountConsumedCents)
            assertEquals(0L, t1.remainingAfterCents) // w1 fully consumed
            assertEquals(BigDecimal("37.037037"), t1.withdrawalRate)

            val t2 = tranches[1]
            assertEquals("w-2", t2.withdrawalId)
            assertEquals(30000L, t2.amountConsumedCents)
            // remainingAfterCents = 500000 - 30000 = 470000
            assertEquals(470000L, t2.remainingAfterCents)
            assertEquals(BigDecimal("36.496350"), t2.withdrawalRate)
        }

        @Test
        fun `withdrawal title is included in tranche when present`() = runTest {
            val namedWithdrawal = withdrawal1.copy(title = "Airport ATM")
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(namedWithdrawal)

            val result = useCase(groupId, currency, 50000L)

            assertTrue(result is CashRatePreviewResult.Available)
            val tranche = (result as CashRatePreviewResult.Available).preview.tranches.first()
            assertEquals("Airport ATM", tranche.withdrawalTitle)
        }

        @Test
        fun `tranches are empty for weighted-average preview when sourceAmount is zero`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1, withdrawal2)

            val result = useCase(groupId, currency, 0L)

            assertTrue(result is CashRatePreviewResult.Available)
            val preview = (result as CashRatePreviewResult.Available).preview
            assertTrue(preview.tranches.isEmpty())
        }

        @Test
        fun `InsufficientCash result carries no tranches`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            // Request more than available
            val result = useCase(groupId, currency, 2000000L)

            assertEquals(CashRatePreviewResult.InsufficientCash, result)
        }
    }

    // ── Scoped pool selection ─────────────────────────────────────────────────

    @Nested
    inner class ScopedPoolSelection {

        private val userId = "user-123"
        private val subunitId = "subunit-456"

        @Test
        fun `USER scope delegates pool query to repository with USER payerType`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.USER, userId)
            } returns listOf(withdrawal1)

            val result = useCase(groupId, currency, 0L, PayerType.USER, userId)

            assertTrue(result is CashRatePreviewResult.Available)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.USER, userId)
            }
        }

        @Test
        fun `SUBUNIT scope delegates pool query to repository with SUBUNIT payerType`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.SUBUNIT, subunitId)
            } returns listOf(withdrawal1)

            val result = useCase(groupId, currency, 0L, PayerType.SUBUNIT, subunitId)

            assertTrue(result is CashRatePreviewResult.Available)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.SUBUNIT, subunitId)
            }
        }

        @Test
        fun `GROUP scope used when no scope specified (default parameters)`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            val result = useCase(groupId, currency, 0L)

            assertTrue(result is CashRatePreviewResult.Available)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            }
        }

        @Test
        fun `returns NoWithdrawals when USER and GROUP pools combined are empty`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.USER, userId)
            } returns emptyList()

            val result = useCase(groupId, currency, 50000L, PayerType.USER, userId)

            assertEquals(CashRatePreviewResult.NoWithdrawals, result)
        }
    }

    // ── Preferred withdrawal scope override ───────────────────────────────────

    @Nested
    inner class PreferredWithdrawalScope {

        @Test
        fun `uses getAvailableWithdrawalsByExactScope when preferredWithdrawalScope is set`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    "user-456"
                )
            } returns listOf(withdrawal1)

            val result = useCase(
                groupId,
                currency,
                0L,
                preferredWithdrawalScope = PayerType.USER,
                preferredWithdrawalOwnerId = "user-456"
            )

            assertTrue(result is CashRatePreviewResult.Available)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    "user-456"
                )
            }
            coVerify(exactly = 0) { cashWithdrawalRepository.getAvailableWithdrawals(any(), any(), any(), any()) }
        }

        @Test
        fun `uses getAvailableWithdrawals fallback when preferredWithdrawalScope is null`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            } returns listOf(withdrawal1)

            val result = useCase(
                groupId,
                currency,
                0L,
                preferredWithdrawalScope = null
            )

            assertTrue(result is CashRatePreviewResult.Available)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, currency, PayerType.GROUP, null)
            }
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(any(), any(), any(), any())
            }
        }

        @Test
        fun `returns NoWithdrawals when exact scope has no funds`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.GROUP,
                    null
                )
            } returns emptyList()

            val result = useCase(
                groupId,
                currency,
                50_000L,
                preferredWithdrawalScope = PayerType.GROUP,
                preferredWithdrawalOwnerId = null
            )

            assertEquals(CashRatePreviewResult.NoWithdrawals, result)
        }

        @Test
        fun `preferred scope FIFO preview uses only exact-scope withdrawals`() = runTest {
            // Only withdrawal1 belongs to USER pool; withdrawal2 is intentionally absent
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    currency,
                    PayerType.USER,
                    "user-456"
                )
            } returns listOf(withdrawal1)

            val result = useCase(
                groupId,
                currency,
                500_000L,
                preferredWithdrawalScope = PayerType.USER,
                preferredWithdrawalOwnerId = "user-456"
            )

            // withdrawal1 has 1_000_000 remaining > 500_000 requested → should succeed
            assertTrue(result is CashRatePreviewResult.Available)
        }
    }
}
