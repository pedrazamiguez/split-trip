package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.AddOn
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
import es.pedrazamiguez.splittrip.domain.usecase.expense.factory.PersistExpenseStrategyFactory
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddExpenseUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var expenseCalculatorService: ExpenseCalculatorService
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var authenticationService: AuthenticationService
    private lateinit var addOnCalculationService: AddOnCalculationService
    private lateinit var useCase: AddExpenseUseCase

    private val groupId = "group-123"
    private val currentUserId = "current-user-456"

    private val baseExpense = Expense(
        id = "expense-1",
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD
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

        useCase = AddExpenseUseCase(
            strategyFactory = strategyFactory
        )
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    inner class Validation {

        @Test
        fun `fails when groupId is null`() = runTest {
            val result = useCase(null, baseExpense)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Group ID") == true)
        }

        @Test
        fun `fails when groupId is blank`() = runTest {
            val result = useCase("  ", baseExpense)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Group ID") == true)
        }

        @Test
        fun `fails when sourceAmount is zero`() = runTest {
            val result = useCase(groupId, baseExpense.copy(sourceAmount = 0L))
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("amount") == true)
        }

        @Test
        fun `fails when title is blank`() = runTest {
            val result = useCase(groupId, baseExpense.copy(title = ""))
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("title") == true)
        }
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    inner class MembershipValidation {

        @Test
        fun `fails with NotGroupMemberException when user is not a member`() = runTest {
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            val result = useCase(groupId, baseExpense)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NotGroupMemberException)
        }

        @Test
        fun `does not save expense when membership check fails`() = runTest {
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            useCase(groupId, baseExpense)

            coVerify(exactly = 0) { expenseRepository.addExpense(any(), any()) }
        }

        @Test
        fun `calls requireMembership with correct groupId on success`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs

            useCase(groupId, baseExpense)

            coVerify(exactly = 1) { groupMembershipService.requireMembership(groupId) }
        }
    }

    // ── Non-cash expense ──────────────────────────────────────────────────────

    @Nested
    inner class NonCashExpense {

        @Test
        fun `saves expense directly without touching withdrawal repository`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs

            val result = useCase(groupId, baseExpense)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { expenseRepository.addExpense(groupId, baseExpense) }
            coVerify(exactly = 0) { cashWithdrawalRepository.getAvailableWithdrawals(any(), any(), any(), any()) }
            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
        }
    }

    // ── Cash expense: batch update ─────────────────────────────────────────────

    @Nested
    inner class CashExpenseBatchUpdate {

        private val cashExpense = baseExpense.copy(
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 23000L
        )

        private val withdrawal1 = CashWithdrawal(
            id = "w-1",
            groupId = groupId,
            amountWithdrawn = 1000000L,
            remainingAmount = 5000L,
            currency = "THB",
            deductedBaseAmount = 26400L,
            createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val withdrawal2 = CashWithdrawal(
            id = "w-2",
            groupId = groupId,
            amountWithdrawn = 500000L,
            remainingAmount = 500000L,
            currency = "THB",
            deductedBaseAmount = 13587L,
            createdAt = LocalDateTime.of(2026, 1, 12, 12, 0)
        )

        private val fifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 621L,
            tranches = listOf(
                CashTranche(withdrawalId = "w-1", amountConsumed = 5000L),
                CashTranche(withdrawalId = "w-2", amountConsumed = 18000L)
            )
        )

        @BeforeEach
        fun setUpCash() {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.GROUP, null)
            } returns listOf(withdrawal1, withdrawal2)

            every {
                expenseCalculatorService.hasInsufficientCash(
                    cashExpense.sourceAmount,
                    listOf(withdrawal1, withdrawal2)
                )
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = cashExpense.sourceAmount,
                    availableWithdrawals = listOf(withdrawal1, withdrawal2)
                )
            } returns fifoResult

            every {
                exchangeRateCalculationService.calculateBlendedRate(
                    sourceAmountCents = cashExpense.sourceAmount,
                    groupAmountCents = fifoResult.groupAmountCents
                )
            } returns BigDecimal("0.027000")

            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
        }

        @Test
        fun `calls updateRemainingAmounts exactly once for multi-tranche expense`() = runTest {
            useCase(groupId, cashExpense)

            // Must be exactly one batch call, never the single-update method
            coVerify(exactly = 1) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmount(any(), any()) }
        }

        @Test
        fun `does NOT update withdrawal amounts when addCashExpense returns false (offline fallback)`() = runTest {
            // addCashExpense returns false = Firestore transaction did not commit
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns false

            useCase(groupId, cashExpense)

            // Withdrawals must NOT be deducted — doing so would create orphaned cloud deductions
            // without a matching expense document (cloud atomicity violation).
            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmount(any(), any()) }
        }

        @Test
        fun `passes groupId to updateRemainingAmounts`() = runTest {
            val groupIdSlot = slot<String>()
            coEvery {
                cashWithdrawalRepository.updateRemainingAmounts(capture(groupIdSlot), any())
            } just Runs

            useCase(groupId, cashExpense)

            assertEquals(groupId, groupIdSlot.captured)
        }

        @Test
        fun `passes correctly deducted withdrawal objects to updateRemainingAmounts`() = runTest {
            val withdrawalsSlot = slot<List<CashWithdrawal>>()
            coEvery {
                cashWithdrawalRepository.updateRemainingAmounts(any(), capture(withdrawalsSlot))
            } just Runs

            useCase(groupId, cashExpense)

            val updated = withdrawalsSlot.captured
            assertEquals(2, updated.size)

            val updatedW1 = updated.first { it.id == "w-1" }
            assertEquals(0L, updatedW1.remainingAmount) // 5000 - 5000

            val updatedW2 = updated.first { it.id == "w-2" }
            assertEquals(482000L, updatedW2.remainingAmount) // 500000 - 18000
        }

        @Test
        fun `attaches tranches and blended group amount to saved expense`() = runTest {
            val savedExpenseSlot = slot<Expense>()
            coEvery {
                expenseRepository.addCashExpense(any(), capture(savedExpenseSlot), any())
            } returns true

            useCase(groupId, cashExpense)

            val saved = savedExpenseSlot.captured
            assertEquals(fifoResult.tranches, saved.cashTranches)
            assertEquals(fifoResult.groupAmountCents, saved.groupAmount)
        }

        @Test
        fun `sets blended exchange rate on saved cash expense`() = runTest {
            val savedExpenseSlot = slot<Expense>()
            coEvery {
                expenseRepository.addCashExpense(any(), capture(savedExpenseSlot), any())
            } returns true

            useCase(groupId, cashExpense)

            val saved = savedExpenseSlot.captured
            // exchangeRate must be the blended rate from FIFO, not the original API rate
            assertEquals(BigDecimal("0.027000"), saved.exchangeRate)
        }

        @Test
        fun `single-tranche cash expense still uses batch call`() = runTest {
            val singleTranche = fifoResult.copy(
                tranches = listOf(CashTranche(withdrawalId = "w-1", amountConsumed = 5000L))
            )
            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(any(), any())
            } returns singleTranche

            useCase(groupId, cashExpense)

            coVerify(exactly = 1) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmount(any(), any()) }
        }

        @Test
        fun `CashConflictException from repository propagates as Result failure`() = runTest {
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } throws CashConflictException()

            val result = useCase(groupId, cashExpense)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is CashConflictException)
        }

        @Test
        fun `does not update withdrawals when CashConflictException is thrown`() = runTest {
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } throws CashConflictException()

            useCase(groupId, cashExpense)

            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
        }
    }

    // ── Insufficient cash ─────────────────────────────────────────────────────

    @Nested
    inner class InsufficientCash {

        private val cashExpense = baseExpense.copy(
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 50000L // More than available
        )

        private val withdrawal = CashWithdrawal(
            id = "w-1",
            groupId = groupId,
            amountWithdrawn = 100000L,
            remainingAmount = 32000L, // Less than required
            currency = "THB",
            deductedBaseAmount = 86400L,
            createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        @BeforeEach
        fun setUpInsufficientCash() {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.GROUP, null)
            } returns listOf(withdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(
                    cashExpense.sourceAmount,
                    listOf(withdrawal)
                )
            } returns true
        }

        @Test
        fun `fails with InsufficientCashException when cash is not enough`() = runTest {
            val result = useCase(groupId, cashExpense)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is InsufficientCashException)
        }

        @Test
        fun `exception carries required and available cent values`() = runTest {
            val result = useCase(groupId, cashExpense)

            val exception = result.exceptionOrNull() as InsufficientCashException
            assertEquals(cashExpense.sourceAmount, exception.requiredCents)
            assertEquals(listOf(withdrawal).sumOf { it.remainingAmount }, exception.availableCents)
        }

        @Test
        fun `does not save expense when cash is insufficient`() = runTest {
            useCase(groupId, cashExpense)

            coVerify(exactly = 0) { expenseRepository.addExpense(any(), any()) }
        }

        @Test
        fun `does not update withdrawals when cash is insufficient`() = runTest {
            useCase(groupId, cashExpense)

            coVerify(exactly = 0) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
        }
    }

    // ── Out-of-pocket: non-cash (standard case) ───────────────────────────────

    @Nested
    inner class OutOfPocketNonCash {

        private val oopExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId,
            paymentMethod = PaymentMethod.CREDIT_CARD
        )

        @Test
        fun `saves expense without FIFO processing`() = runTest {
            val result = useCase(groupId, oopExpense)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { expenseRepository.addExpense(groupId, oopExpense) }
            coVerify(exactly = 0) { cashWithdrawalRepository.getAvailableWithdrawals(any(), any(), any(), any()) }
        }

        @Test
        fun `creates paired contribution with correct linked expense id`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(oopExpense.id, contributionSlot.captured.linkedExpenseId)
        }

        @Test
        fun `paired contribution amount equals group amount when no add-ons`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(oopExpense.groupAmount, contributionSlot.captured.amount)
        }

        @Test
        fun `paired contribution uses group currency`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(oopExpense.groupCurrency, contributionSlot.captured.currency)
        }
    }

    // ── Out-of-pocket: cash (FIFO runs from USER-scoped pool) ────────────────

    @Nested
    inner class OutOfPocketCash {

        private val oopCashExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId,
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "EUR"
        )

        private val userWithdrawal = CashWithdrawal(
            id = "w-user-1",
            groupId = groupId,
            amountWithdrawn = 10000L,
            remainingAmount = 10000L,
            currency = "EUR",
            deductedBaseAmount = 10000L,
            withdrawalScope = PayerType.USER,
            withdrawnBy = currentUserId,
            createdAt = java.time.LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val userFifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 5000L,
            tranches = listOf(CashTranche(withdrawalId = "w-user-1", amountConsumed = 5000L))
        )

        @BeforeEach
        fun setUpUserCash() {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "EUR", PayerType.USER, currentUserId)
            } returns listOf(userWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(oopCashExpense.sourceAmount, listOf(userWithdrawal))
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = oopCashExpense.sourceAmount,
                    availableWithdrawals = listOf(userWithdrawal)
                )
            } returns userFifoResult

            every {
                exchangeRateCalculationService.calculateBlendedRate(any(), any())
            } returns BigDecimal("1.000000")

            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
        }

        @Test
        fun `runs FIFO processing from USER-scoped withdrawal pool`() = runTest {
            val result = useCase(groupId, oopCashExpense)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "EUR", PayerType.USER, currentUserId)
            }
            coVerify(exactly = 1) { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) }
        }

        @Test
        fun `attaches tranches and FIFO blended group amount to saved USER cash expense`() = runTest {
            val savedSlot = slot<Expense>()
            coEvery { expenseRepository.addCashExpense(any(), capture(savedSlot), any()) } returns true

            useCase(groupId, oopCashExpense)

            val saved = savedSlot.captured
            assertEquals(userFifoResult.tranches, saved.cashTranches)
            assertEquals(userFifoResult.groupAmountCents, saved.groupAmount)
        }

        @Test
        fun `creates paired contribution for USER cash expense`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopCashExpense)

            assertEquals(oopCashExpense.id, contributionSlot.captured.linkedExpenseId)
            assertEquals(PayerType.USER, contributionSlot.captured.contributionScope)
        }
    }

    // ── Out-of-pocket: with add-ons (effective amount) ────────────────────────

    @Nested
    inner class OutOfPocketWithAddOns {

        private val tipAddOn = AddOn(
            type = AddOnType.TIP,
            mode = AddOnMode.ON_TOP,
            groupAmountCents = 500L
        )

        private val oopExpenseWithAddOns = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId,
            addOns = listOf(tipAddOn)
        )

        @Test
        fun `paired contribution uses effective amount (base plus add-ons)`() = runTest {
            val effectiveAmount = 5500L
            every {
                addOnCalculationService.calculateEffectiveGroupAmount(
                    oopExpenseWithAddOns.groupAmount,
                    oopExpenseWithAddOns.addOns
                )
            } returns effectiveAmount

            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpenseWithAddOns)

            assertEquals(effectiveAmount, contributionSlot.captured.amount)
        }

        @Test
        fun `calls calculateEffectiveGroupAmount with expense data`() = runTest {
            useCase(groupId, oopExpenseWithAddOns)

            io.mockk.verify(exactly = 1) {
                addOnCalculationService.calculateEffectiveGroupAmount(
                    oopExpenseWithAddOns.groupAmount,
                    oopExpenseWithAddOns.addOns
                )
            }
        }
    }

    // ── Out-of-pocket: multi-currency ─────────────────────────────────────────

    @Nested
    inner class OutOfPocketMultiCurrency {

        private val oopMultiCurrencyExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId,
            sourceAmount = 500000L,
            sourceCurrency = "THB",
            groupAmount = 1350L,
            groupCurrency = "EUR",
            exchangeRate = BigDecimal("0.027000")
        )

        @Test
        fun `paired contribution uses group amount in group currency`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopMultiCurrencyExpense)

            assertEquals(1350L, contributionSlot.captured.amount)
            assertEquals("EUR", contributionSlot.captured.currency)
        }
    }

    // ── Out-of-pocket: contribution field validation ──────────────────────────

    @Nested
    inner class OutOfPocketContributionFields {

        private val oopExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId
        )

        @Test
        fun `paired contribution has USER scope`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(PayerType.USER, contributionSlot.captured.contributionScope)
        }

        @Test
        fun `paired contribution userId matches payer`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(currentUserId, contributionSlot.captured.userId)
            assertEquals(currentUserId, contributionSlot.captured.createdBy)
        }

        @Test
        fun `createdBy is actor not payer when payerId differs from authenticated user`() = runTest {
            val otherPayerId = "other-user-789"
            val impersonatedExpense = oopExpense.copy(payerId = otherPayerId)
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, impersonatedExpense)

            assertEquals(otherPayerId, contributionSlot.captured.userId)
            assertEquals(currentUserId, contributionSlot.captured.createdBy)
        }

        @Test
        fun `generates expense ID and links paired contribution when expense has blank id`() = runTest {
            val blankIdExpense = oopExpense.copy(id = "")
            val expenseSlot = slot<Expense>()
            val contributionSlot = slot<Contribution>()
            coEvery { expenseRepository.addExpense(any(), capture(expenseSlot)) } just Runs
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, blankIdExpense)

            assertTrue(expenseSlot.captured.id.isNotBlank())
            assertEquals(expenseSlot.captured.id, contributionSlot.captured.linkedExpenseId)
        }

        @Test
        fun `paired contribution has non-blank UUID id`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertTrue(contributionSlot.captured.id.isNotBlank())
        }

        @Test
        fun `falls back to authenticationService when payerId is null`() = runTest {
            val expenseWithoutPayerId = oopExpense.copy(payerId = null)
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, expenseWithoutPayerId)

            assertEquals(currentUserId, contributionSlot.captured.userId)
        }

        @Test
        fun `passes correct groupId when adding paired contribution`() = runTest {
            val groupIdSlot = slot<String>()
            coEvery {
                contributionRepository.addContribution(capture(groupIdSlot), any())
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(groupId, groupIdSlot.captured)
        }

        @Test
        fun `paired contribution created for scheduled out-of-pocket expense`() = runTest {
            val scheduledOopExpense = oopExpense.copy(
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = LocalDateTime.now().plusDays(30)
            )
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, scheduledOopExpense)

            assertEquals(scheduledOopExpense.id, contributionSlot.captured.linkedExpenseId)
        }
    }

    // ── Out-of-pocket: atomicity (compensating rollback) ──────────────────────

    @Nested
    inner class OutOfPocketAtomicity {

        private val oopExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId
        )

        @Test
        fun `rolls back expense when paired contribution creation fails`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs
            coEvery {
                contributionRepository.addContribution(any(), any())
            } throws RuntimeException("Contribution failed")
            coEvery { expenseRepository.deleteExpense(any(), any()) } just Runs

            val result = useCase(groupId, oopExpense)

            assertTrue(result.isFailure)
            coVerify(exactly = 1) { expenseRepository.deleteExpense(groupId, oopExpense.id) }
        }

        @Test
        fun `suppresses rollback exception when both contribution and delete fail`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs
            coEvery {
                contributionRepository.addContribution(any(), any())
            } throws RuntimeException("Contribution failed")
            coEvery {
                expenseRepository.deleteExpense(any(), any())
            } throws RuntimeException("Rollback failed")

            val result = useCase(groupId, oopExpense)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()!!
            assertEquals("Contribution failed", exception.message)
            assertTrue(exception.suppressed.any { it.message == "Rollback failed" })
        }
    }

    // ── Out-of-pocket: contribution scope selection ──────────────────────────

    @Nested
    inner class OutOfPocketWithScope {

        private val oopExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId
        )

        @Test
        fun `paired contribution uses SUBUNIT scope and subunitId when specified`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.SUBUNIT,
                pairedSubunitId = "subunit-1"
            )

            assertEquals(PayerType.SUBUNIT, contributionSlot.captured.contributionScope)
            assertEquals("subunit-1", contributionSlot.captured.subunitId)
        }

        @Test
        fun `paired contribution uses GROUP scope and null subunitId when specified`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.GROUP,
                pairedSubunitId = null
            )

            assertEquals(PayerType.GROUP, contributionSlot.captured.contributionScope)
            assertEquals(null, contributionSlot.captured.subunitId)
        }

        @Test
        fun `paired contribution defaults to USER scope when no scope specified`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(groupId, oopExpense)

            assertEquals(PayerType.USER, contributionSlot.captured.contributionScope)
            assertEquals(null, contributionSlot.captured.subunitId)
        }
    }

    // ── Out-of-pocket: scope/subunit sanitization ─────────────────────────────

    @Nested
    inner class OutOfPocketScopeSanitization {

        private val oopExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId
        )

        @Test
        fun `fails when SUBUNIT scope has null subunitId`() = runTest {
            val result = useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.SUBUNIT,
                pairedSubunitId = null
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("SUBUNIT") == true)
            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `fails when SUBUNIT scope has blank subunitId`() = runTest {
            val result = useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.SUBUNIT,
                pairedSubunitId = "   "
            )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("SUBUNIT") == true)
            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `rolls back expense when SUBUNIT scope validation fails`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs
            coEvery { expenseRepository.deleteExpense(any(), any()) } just Runs

            val result = useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.SUBUNIT,
                pairedSubunitId = null
            )

            assertTrue(result.isFailure)
            coVerify(exactly = 1) { expenseRepository.deleteExpense(groupId, oopExpense.id) }
        }

        @Test
        fun `sanitizes subunitId to null for GROUP scope`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.GROUP,
                pairedSubunitId = "stale-subunit-id"
            )

            assertEquals(PayerType.GROUP, contributionSlot.captured.contributionScope)
            assertEquals(null, contributionSlot.captured.subunitId)
        }

        @Test
        fun `sanitizes subunitId to null for USER scope`() = runTest {
            val contributionSlot = slot<Contribution>()
            coEvery {
                contributionRepository.addContribution(any(), capture(contributionSlot))
            } just Runs

            useCase(
                groupId,
                oopExpense,
                pairedContributionScope = PayerType.USER,
                pairedSubunitId = "stale-subunit-id"
            )

            assertEquals(PayerType.USER, contributionSlot.captured.contributionScope)
            assertEquals(null, contributionSlot.captured.subunitId)
        }
    }

    // ── GROUP-funded: no paired contribution ──────────────────────────────────

    @Nested
    inner class GroupFundedNoContribution {

        @Test
        fun `does not create paired contribution for GROUP non-cash expense`() = runTest {
            useCase(groupId, baseExpense)

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }

        @Test
        fun `preserves existing behavior for GROUP non-cash expense`() = runTest {
            coEvery { expenseRepository.addExpense(any(), any()) } just Runs

            val result = useCase(groupId, baseExpense)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { expenseRepository.addExpense(groupId, baseExpense) }
        }
    }

    // ── GROUP-funded + CASH: existing FIFO still works ────────────────────────

    @Nested
    inner class GroupFundedCashStillFIFO {

        private val groupCashExpense = baseExpense.copy(
            payerType = PayerType.GROUP,
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 10000L
        )

        private val withdrawal = CashWithdrawal(
            id = "w-1",
            groupId = groupId,
            amountWithdrawn = 500000L,
            remainingAmount = 500000L,
            currency = "THB",
            deductedBaseAmount = 13500L,
            createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val fifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 270L,
            tranches = listOf(CashTranche(withdrawalId = "w-1", amountConsumed = 10000L))
        )

        @BeforeEach
        fun setUpGroupCash() {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.GROUP, null)
            } returns listOf(withdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(any(), any())
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(any(), any())
            } returns fifoResult

            every {
                exchangeRateCalculationService.calculateBlendedRate(any(), any())
            } returns BigDecimal("0.027000")

            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
        }

        @Test
        fun `triggers FIFO for GROUP-funded cash expense`() = runTest {
            useCase(groupId, groupCashExpense)

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.GROUP, null)
            }
            coVerify(exactly = 1) {
                cashWithdrawalRepository.updateRemainingAmounts(any(), any())
            }
        }

        @Test
        fun `does not create paired contribution for GROUP cash expense`() = runTest {
            useCase(groupId, groupCashExpense)

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }
    }

    // ── USER cash expense: GROUP pool fallback ────────────────────────────────

    @Nested
    inner class UserCashExpenseFallbackToGroup {

        private val userCashExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId,
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 10000L
        )

        private val groupWithdrawal = CashWithdrawal(
            id = "w-group-1",
            groupId = groupId,
            amountWithdrawn = 500000L,
            remainingAmount = 500000L,
            currency = "THB",
            deductedBaseAmount = 13500L,
            withdrawalScope = PayerType.GROUP,
            createdAt = java.time.LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val fifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 270L,
            tranches = listOf(CashTranche(withdrawalId = "w-group-1", amountConsumed = 10000L))
        )

        @BeforeEach
        fun setUp() {
            // Simulate: USER pool empty + GROUP fallback returned by repository (merged list)
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.USER, currentUserId)
            } returns listOf(groupWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(userCashExpense.sourceAmount, listOf(groupWithdrawal))
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = userCashExpense.sourceAmount,
                    availableWithdrawals = listOf(groupWithdrawal)
                )
            } returns fifoResult

            every { exchangeRateCalculationService.calculateBlendedRate(any(), any()) } returns BigDecimal("0.027000")
            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
        }

        @Test
        fun `queries USER-scoped pool (repository handles GROUP fallback internally)`() = runTest {
            useCase(groupId, userCashExpense)

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.USER, currentUserId)
            }
        }

        @Test
        fun `attaches tranches from fallback GROUP pool`() = runTest {
            val savedSlot = slot<Expense>()
            coEvery { expenseRepository.addCashExpense(any(), capture(savedSlot), any()) } returns true

            useCase(groupId, userCashExpense)

            val saved = savedSlot.captured
            assertEquals(fifoResult.tranches, saved.cashTranches)
            assertEquals(fifoResult.groupAmountCents, saved.groupAmount)
        }
    }

    // ── SUBUNIT cash expense: FIFO from SUBUNIT-scoped pool ──────────────────

    @Nested
    inner class SubunitCashExpenseFifo {

        private val subunitId = "subunit-42"
        private val subunitCashExpense = baseExpense.copy(
            payerType = PayerType.SUBUNIT,
            payerId = subunitId,
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 10000L
        )

        private val subunitWithdrawal = CashWithdrawal(
            id = "w-sub-1",
            groupId = groupId,
            amountWithdrawn = 500000L,
            remainingAmount = 500000L,
            currency = "THB",
            deductedBaseAmount = 13500L,
            withdrawalScope = PayerType.SUBUNIT,
            subunitId = subunitId,
            createdAt = java.time.LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val fifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 270L,
            tranches = listOf(CashTranche(withdrawalId = "w-sub-1", amountConsumed = 10000L))
        )

        @BeforeEach
        fun setUp() {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.SUBUNIT, subunitId)
            } returns listOf(subunitWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(subunitCashExpense.sourceAmount, listOf(subunitWithdrawal))
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = subunitCashExpense.sourceAmount,
                    availableWithdrawals = listOf(subunitWithdrawal)
                )
            } returns fifoResult

            every { exchangeRateCalculationService.calculateBlendedRate(any(), any()) } returns BigDecimal("0.027000")
            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
        }

        @Test
        fun `queries SUBUNIT-scoped pool for SUBUNIT cash expense`() = runTest {
            useCase(groupId, subunitCashExpense)

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.SUBUNIT, subunitId)
            }
        }

        @Test
        fun `FIFO runs and attaches tranches for SUBUNIT cash expense`() = runTest {
            val savedSlot = slot<Expense>()
            coEvery { expenseRepository.addCashExpense(any(), capture(savedSlot), any()) } returns true

            useCase(groupId, subunitCashExpense)

            val saved = savedSlot.captured
            assertEquals(fifoResult.tranches, saved.cashTranches)
            assertEquals(fifoResult.groupAmountCents, saved.groupAmount)
        }

        @Test
        fun `does not create paired contribution for SUBUNIT cash expense`() = runTest {
            useCase(groupId, subunitCashExpense)

            coVerify(exactly = 0) { contributionRepository.addContribution(any(), any()) }
        }
    }

    // ── USER cash expense: InsufficientCashException when both pools empty ────

    @Nested
    inner class UserCashExpenseInsufficientBothPools {

        private val userCashExpense = baseExpense.copy(
            payerType = PayerType.USER,
            payerId = currentUserId,
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 100000L
        )

        @BeforeEach
        fun setUp() {
            // Combined pool (USER + GROUP fallback) is insufficient
            val tinyWithdrawal = CashWithdrawal(
                id = "w-tiny",
                groupId = groupId,
                amountWithdrawn = 100000L,
                remainingAmount = 5000L,
                currency = "THB",
                deductedBaseAmount = 135L,
                createdAt = java.time.LocalDateTime.of(2026, 1, 10, 12, 0)
            )
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(groupId, "THB", PayerType.USER, currentUserId)
            } returns listOf(tinyWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(userCashExpense.sourceAmount, listOf(tinyWithdrawal))
            } returns true
        }

        @Test
        fun `throws InsufficientCashException when USER and GROUP pools combined cannot cover expense`() = runTest {
            val result = useCase(groupId, userCashExpense)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is InsufficientCashException)
        }

        @Test
        fun `does not save expense when combined pools are insufficient`() = runTest {
            useCase(groupId, userCashExpense)

            coVerify(exactly = 0) { expenseRepository.addCashExpense(any(), any(), any()) }
        }
    }

    // ── Preferred withdrawal scope override ───────────────────────────────────

    @Nested
    inner class PreferredWithdrawalScope {

        private val cashExpense = baseExpense.copy(
            paymentMethod = PaymentMethod.CASH,
            sourceCurrency = "THB",
            sourceAmount = 10_000L
        )

        private val personalWithdrawal = CashWithdrawal(
            id = "pw-1",
            groupId = groupId,
            amountWithdrawn = 500_000L,
            remainingAmount = 50_000L,
            currency = "THB",
            deductedBaseAmount = 13_500L,
            createdAt = LocalDateTime.of(2026, 1, 10, 12, 0)
        )

        private val fifoResult = ExpenseCalculatorService.FifoCashResult(
            groupAmountCents = 270L,
            tranches = listOf(CashTranche(withdrawalId = "pw-1", amountConsumed = 10_000L))
        )

        @BeforeEach
        fun setUpPreferred() {
            coEvery { expenseRepository.addCashExpense(any(), any(), any()) } returns true
            coEvery { cashWithdrawalRepository.updateRemainingAmounts(any(), any()) } just Runs
        }

        @Test
        fun `uses getAvailableWithdrawalsByExactScope when preferredWithdrawalScope is set`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    "THB",
                    PayerType.USER,
                    currentUserId
                )
            } returns listOf(personalWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(cashExpense.sourceAmount, listOf(personalWithdrawal))
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = cashExpense.sourceAmount,
                    availableWithdrawals = listOf(personalWithdrawal)
                )
            } returns fifoResult

            every {
                exchangeRateCalculationService.calculateBlendedRate(any(), any())
            } returns BigDecimal("0.027000")

            useCase(
                groupId,
                cashExpense,
                preferredWithdrawalScope = PayerType.USER,
                preferredWithdrawalOwnerId = currentUserId
            )

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    "THB",
                    PayerType.USER,
                    currentUserId
                )
            }
            // Must NOT fall back to the scoped-fallback method
            coVerify(exactly = 0) { cashWithdrawalRepository.getAvailableWithdrawals(any(), any(), any(), any()) }
        }

        @Test
        fun `uses getAvailableWithdrawals fallback when preferredWithdrawalScope is null`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawals(
                    groupId,
                    "THB",
                    cashExpense.payerType,
                    cashExpense.payerId
                )
            } returns listOf(personalWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(cashExpense.sourceAmount, listOf(personalWithdrawal))
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = cashExpense.sourceAmount,
                    availableWithdrawals = listOf(personalWithdrawal)
                )
            } returns fifoResult

            every {
                exchangeRateCalculationService.calculateBlendedRate(any(), any())
            } returns BigDecimal("0.027000")

            useCase(groupId, cashExpense, preferredWithdrawalScope = null)

            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawals(
                    groupId,
                    "THB",
                    cashExpense.payerType,
                    cashExpense.payerId
                )
            }
            coVerify(exactly = 0) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(any(), any(), any(), any())
            }
        }

        @Test
        fun `preferred GROUP scope queries GROUP pool exactly without fallback`() = runTest {
            coEvery {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    "THB",
                    PayerType.GROUP,
                    null
                )
            } returns listOf(personalWithdrawal)

            every {
                expenseCalculatorService.hasInsufficientCash(cashExpense.sourceAmount, listOf(personalWithdrawal))
            } returns false

            coEvery {
                expenseCalculatorService.calculateFifoCashAmount(
                    amountToCover = cashExpense.sourceAmount,
                    availableWithdrawals = listOf(personalWithdrawal)
                )
            } returns fifoResult

            every {
                exchangeRateCalculationService.calculateBlendedRate(any(), any())
            } returns BigDecimal("0.027000")

            val result = useCase(
                groupId,
                cashExpense,
                preferredWithdrawalScope = PayerType.GROUP,
                preferredWithdrawalOwnerId = null
            )

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                    groupId,
                    "THB",
                    PayerType.GROUP,
                    null
                )
            }
        }
    }
}
