package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateExpenseUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var expenseCalculatorService: ExpenseCalculatorService
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var addOnCalculationService: AddOnCalculationService
    private lateinit var useCase: UpdateExpenseUseCase

    private val groupId = "group-123"
    private val currentUserId = "current-user-456"
    private val expenseId = "expense-1"

    private val originalExpense = Expense(
        id = expenseId,
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD
    )

    private val updatedExpense = originalExpense.copy(
        title = "Updated Dinner",
        sourceAmount = 7500L,
        groupAmount = 7500L
    )

    @BeforeEach
    fun setUp() {
        expenseRepository = mockk(relaxed = true)
        cashWithdrawalRepository = mockk(relaxed = true)
        expenseCalculatorService = mockk()
        exchangeRateCalculationService = mockk(relaxed = true)
        groupMembershipService = mockk()
        contributionRepository = mockk(relaxed = true)
        authenticationService = mockk()
        addOnCalculationService = mockk()

        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        every { authenticationService.requireUserId() } returns currentUserId
        every { addOnCalculationService.calculateEffectiveGroupAmount(any(), any()) } answers {
            firstArg()
        }
        coEvery { expenseRepository.getExpenseById(expenseId) } returns originalExpense
        coEvery { contributionRepository.findByLinkedExpenseId(groupId, expenseId) } returns null

        val strategyFactory = PersistExpenseStrategyFactory(
            expenseRepository = expenseRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            expenseCalculatorService = expenseCalculatorService,
            exchangeRateCalculationService = exchangeRateCalculationService,
            groupMembershipService = groupMembershipService,
            contributionRepository = contributionRepository,
            authenticationService = authenticationService,
            addOnCalculationService = addOnCalculationService
        )

        useCase = UpdateExpenseUseCase(strategyFactory = strategyFactory)
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    inner class Validation {

        @Test
        fun `fails when groupId is null`() = runTest {
            val result = useCase(null, updatedExpense)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertTrue(result.exceptionOrNull()?.message?.contains("Group ID") == true)
        }

        @Test
        fun `fails when groupId is blank`() = runTest {
            val result = useCase("   ", updatedExpense)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Group ID") == true)
        }

        @Test
        fun `fails when expense id is blank`() = runTest {
            val result = useCase(groupId, updatedExpense.copy(id = ""))
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("ID") == true)
        }

        @Test
        fun `fails when sourceAmount is zero`() = runTest {
            val result = useCase(groupId, updatedExpense.copy(sourceAmount = 0L))
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("amount") == true)
        }

        @Test
        fun `fails when title is blank`() = runTest {
            val result = useCase(groupId, updatedExpense.copy(title = "  "))
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("title") == true)
        }

        @Test
        fun `fails when original expense is not found`() = runTest {
            coEvery { expenseRepository.getExpenseById(expenseId) } returns null

            val result = useCase(groupId, updatedExpense)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
            assertTrue(result.exceptionOrNull()?.message?.contains(expenseId) == true)
        }
    }

    // ── Membership ────────────────────────────────────────────────────────────

    @Nested
    inner class MembershipValidation {

        @Test
        fun `fails with NotGroupMemberException when user is not a member`() = runTest {
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = currentUserId)

            val result = useCase(groupId, updatedExpense)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NotGroupMemberException)
            coVerify(exactly = 0) { expenseRepository.addExpense(any(), any()) }
            coVerify(exactly = 0) { expenseRepository.addCashExpense(any(), any(), any()) }
        }
    }

    // ── Non-cash update ───────────────────────────────────────────────────────

    @Nested
    inner class NonCashUpdate {

        @Test
        fun `saves expense directly without touching withdrawal repository for GROUP non-cash`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs

            val result = useCase(groupId, updatedExpense)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { expenseRepository.addExpense(groupId, updatedExpense) }
            coVerify(exactly = 0) { cashWithdrawalRepository.getAvailableWithdrawals(any(), any(), any(), any()) }
        }

        @Test
        fun `does not create paired contribution for GROUP non-cash update`() = runTest {
            useCase(groupId, updatedExpense)

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `deletes existing paired contribution before processing`() = runTest {
            useCase(groupId, updatedExpense)

            coVerify(exactly = 1) { contributionRepository.deleteByLinkedExpenseId(groupId, expenseId) }
        }
    }

    // ── Out-of-pocket update (USER, non-cash) ─────────────────────────────────

    @Nested
    inner class OutOfPocketUpdate {

        private val oopUpdated = updatedExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId
        )

        @Test
        fun `creates new paired contribution for USER expense after deleting old one`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            val result = useCase(groupId, oopUpdated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { contributionRepository.deleteByLinkedExpenseId(groupId, expenseId) }
            assertEquals(oopUpdated.id, contributionSlot.captured.linkedExpenseId)
            assertEquals(PayerType.USER, contributionSlot.captured.contributionScope)
            assertEquals(currentUserId, contributionSlot.captured.userId)
        }

        @Test
        fun `paired contribution uses SUBUNIT scope when specified`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(
                groupId,
                oopUpdated,
                pairedContributionScope = PayerType.SUBUNIT,
                pairedSubunitId = "subunit-9"
            )

            assertEquals(PayerType.SUBUNIT, contributionSlot.captured.contributionScope)
            assertEquals("subunit-9", contributionSlot.captured.subunitId)
        }

        @Test
        fun `fails when SUBUNIT scope has blank subunitId`() = runTest {
            val result = useCase(
                groupId,
                oopUpdated,
                pairedContributionScope = PayerType.SUBUNIT,
                pairedSubunitId = ""
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("SUBUNIT") == true)
        }
    }

    // ── Cash update: refunds original tranches and applies new FIFO ───────────

    @Nested
    inner class CashUpdate {

        private val originalTranches = listOf(
            CashTranche(withdrawalId = "w-1", amountConsumed = 5000L),
            CashTranche(withdrawalId = "w-2", amountConsumed = 3000L)
        )

        private val originalCashExpense = originalExpense.copy(
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 8000L,
            cashTranches = originalTranches
        )

        private val updatedCashExpense = originalCashExpense.copy(
            title = "Updated Dinner",
            sourceAmount = 10000L
        )

        private val withdrawal1 = CashWithdrawal(
            id = "w-1",
            groupId = groupId,
            amountWithdrawn = 100000L,
            remainingAmount = 50000L,
            currency = "THB",
            deductedBaseAmount = 2700L,
            createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val withdrawal2 = CashWithdrawal(
            id = "w-2",
            groupId = groupId,
            amountWithdrawn = 100000L,
            remainingAmount = 80000L,
            currency = "THB",
            deductedBaseAmount = 2700L,
            createdAt = LocalDateTime.of(2026, 1, 12, 12, 0)
        )

        private val fifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 270L,
            tranches = listOf(CashTranche(withdrawalId = "w-1", amountConsumed = 10000L))
        )

        @BeforeEach
        fun setUpCash() {
            coEvery { expenseRepository.getExpenseById(expenseId) } returns originalCashExpense
            coEvery {
                cashWithdrawalRepository.getWithdrawalById("w-1")
            } returns withdrawal1
            coEvery {
                cashWithdrawalRepository.getWithdrawalById("w-2")
            } returns withdrawal2

            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.GROUP, null)
            } returns listOf(withdrawal1, withdrawal2)

            every { expenseCalculatorService.hasInsufficientCash(any(), any()) } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(any(), any())
            } returns fifoResult

            every { exchangeRateCalculationService.calculateBlendedRate(any(), any()) } returns BigDecimal("0.027000")

            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
            coEvery { cashWithdrawalRepository.refundTranche(any(), any()) } just Runs
        }

        @Test
        fun `refunds each original tranche locally before applying new FIFO`() = runTest {
            useCase(groupId, updatedCashExpense)

            coVerify(exactly = 1) { cashWithdrawalRepository.refundTranche("w-1", 5000L) }
            coVerify(exactly = 1) { cashWithdrawalRepository.refundTranche("w-2", 3000L) }
        }

        @Test
        fun `applies new FIFO and updates remaining amounts when transaction commits`() = runTest {
            useCase(groupId, updatedCashExpense)

            coVerify(exactly = 1) { expenseRepository.addCashExpense(groupId, any(), any()) }
            coVerify(exactly = 1) { cashWithdrawalRepository.updateRemainingAmounts(groupId, any()) }
        }

        @Test
        fun `attaches new tranches and blended group amount to saved expense`() = runTest {
            val savedSlot = slot<Expense>()
            coEvery {
                expenseRepository.addCashExpense(any(), capture(savedSlot), any())
            } returns true

            useCase(groupId, updatedCashExpense)

            assertEquals(fifoResult.tranches, savedSlot.captured.cashTranches)
            assertEquals(fifoResult.groupAmountCents, savedSlot.captured.groupAmount)
            assertEquals(BigDecimal("0.027000"), savedSlot.captured.exchangeRate)
        }

        @Test
        fun `offline fallback restores original withdrawals when transaction does not commit`() = runTest {
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns false

            useCase(groupId, updatedCashExpense)

            val capturedSlot = slot<List<CashWithdrawal>>()
            coVerify(exactly = 1) {
                cashWithdrawalRepository.updateRemainingAmounts(eq(groupId), capture(capturedSlot))
            }
            val ids = capturedSlot.captured.map { it.id }.toSet()
            assertTrue(ids.contains("w-1"))
            assertTrue(ids.contains("w-2"))
        }

        @Test
        fun `does not update withdrawals on offline fallback when there are no original withdrawals`() = runTest {
            val expenseWithoutTranches = originalCashExpense.copy(cashTranches = emptyList())
            coEvery { expenseRepository.getExpenseById(expenseId) } returns expenseWithoutTranches
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns false

            useCase(groupId, updatedCashExpense)

            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
        }
    }

    // ── Rollback on persistence failure ───────────────────────────────────────

    @Nested
    inner class Rollback {

        private val originalContribution = Contribution(
            id = "c-1",
            groupId = groupId,
            userId = currentUserId,
            createdBy = currentUserId,
            contributionScope = PayerType.USER,
            amount = 5000L,
            currency = "EUR",
            linkedExpenseId = expenseId
        )

        @Test
        fun `rolls back to original state when paired contribution creation fails`() = runTest {
            val oopUpdated = updatedExpense.copy(
                payerType = PayerType.USER,
                payerId = currentUserId
            )

            coEvery {
                expenseRepository.getExpenseById(expenseId)
            } returns originalExpense.copy(payerType = PayerType.USER, payerId = currentUserId)
            coEvery {
                contributionRepository.findByLinkedExpenseId(groupId, expenseId)
            } returns originalContribution
            coEvery {
                contributionRepository.addContribution(any(), any())
            } throws RuntimeException("Contribution failed")

            val result = useCase(groupId, oopUpdated)

            assertTrue(result.isFailure)
            assertEquals("Contribution failed", result.exceptionOrNull()?.message)

            // Original expense and contribution restored
            coVerify(atLeast = 1) { expenseRepository.addExpense(groupId, any()) }
            coVerify(exactly = 1) { contributionRepository.addContribution(groupId, originalContribution) }
        }

        @Test
        fun `rollback suppresses secondary exceptions on restore failure`() = runTest {
            val oopUpdated = updatedExpense.copy(
                payerType = PayerType.USER,
                payerId = currentUserId
            )

            coEvery {
                expenseRepository.getExpenseById(expenseId)
            } returns originalExpense.copy(payerType = PayerType.USER, payerId = currentUserId)
            coEvery {
                contributionRepository.findByLinkedExpenseId(groupId, expenseId)
            } returns originalContribution
            coEvery {
                contributionRepository.addContribution(any(), any())
            } throws RuntimeException("Primary failure")

            // First call to addExpense (new persist) succeeds; second call (restore in rollback) throws.
            var addExpenseCallCount = 0
            coEvery { expenseRepository.addExpense(any(), any()) } answers {
                addExpenseCallCount++
                if (addExpenseCallCount > 1) {
                    throw RuntimeException("Restore expense failed")
                }
            }

            val result = useCase(groupId, oopUpdated)

            assertTrue(result.isFailure)
            val ex = result.exceptionOrNull()
            assertNotNull(ex)
            assertEquals("Primary failure", ex?.message)
            assertTrue(ex!!.suppressed.any { it.message == "Restore expense failed" })
        }
    }
}
