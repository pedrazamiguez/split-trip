package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.exception.NotGroupMemberException
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.expense.impl.DeleteExpenseUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DeleteExpenseUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var groupMembershipService: GroupMembershipService
    private lateinit var contributionRepository: ContributionRepository
    private lateinit var useCase: DeleteExpenseUseCase

    @BeforeEach
    fun setUp() {
        expenseRepository = mockk()
        cashWithdrawalRepository = mockk(relaxed = true)
        groupMembershipService = mockk()
        contributionRepository = mockk(relaxed = true)
        coEvery { groupMembershipService.requireMembership(any()) } just Runs
        useCase = DeleteExpenseUseCaseImpl(
            expenseRepository,
            cashWithdrawalRepository,
            groupMembershipService,
            contributionRepository
        )
    }

    // ── Membership validation ─────────────────────────────────────────────────

    @Nested
    inner class MembershipValidation {

        @Test
        fun `throws NotGroupMemberException when user is not a member`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When / Then
            try {
                useCase(groupId, expenseId)
                fail("Expected NotGroupMemberException to be thrown")
            } catch (e: NotGroupMemberException) {
                assertTrue(e.groupId == groupId)
            }
        }

        @Test
        fun `does not delete expense when membership check fails`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            coEvery {
                groupMembershipService.requireMembership(groupId)
            } throws NotGroupMemberException(groupId = groupId, userId = "user-123")

            // When
            runCatching { useCase(groupId, expenseId) }

            // Then
            coVerify(exactly = 0) { expenseRepository.getExpenseById(any()) }
            coVerify(exactly = 0) { expenseRepository.deleteExpense(any(), any()) }
        }

        @Test
        fun `calls requireMembership before deleting`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            coEvery { expenseRepository.getExpenseById(expenseId) } returns null
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then
            coVerify(exactly = 1) { groupMembershipService.requireMembership(groupId) }
        }
    }

    @Nested
    inner class Invocation {

        @Test
        fun `delegates to repository deleteExpense`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            coEvery { expenseRepository.getExpenseById(expenseId) } returns null
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then
            coVerify(exactly = 1) { expenseRepository.deleteExpense(groupId, expenseId) }
        }

        @Test
        fun `passes correct groupId and expenseId to repository`() = runTest {
            // Given
            val groupId = "specific-group-id-789"
            val expenseId = "specific-expense-id-012"
            coEvery { expenseRepository.getExpenseById(any()) } returns null
            coEvery { expenseRepository.deleteExpense(any(), any()) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then
            coVerify { expenseRepository.deleteExpense(groupId, expenseId) }
        }

        @Test
        fun `propagates exception from repository`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            val exception = RuntimeException("Delete failed")
            coEvery { expenseRepository.getExpenseById(any()) } returns null
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } throws exception

            // When/Then
            try {
                useCase(groupId, expenseId)
                fail("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertTrue(e.message == "Delete failed")
            }
        }
    }

    @Nested
    inner class CashTrancheRefund {

        @Test
        fun `refunds cash tranches when deleting cash expense`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            val expense = Expense(
                id = expenseId,
                groupId = groupId,
                title = "Souvenir",
                sourceAmount = 23000L,
                sourceCurrency = "THB",
                groupAmount = 621L,
                groupCurrency = "EUR",
                paymentMethod = PaymentMethod.CASH,
                cashTranches = listOf(
                    CashTranche(withdrawalId = "w-1", amountConsumed = 5000L),
                    CashTranche(withdrawalId = "w-2", amountConsumed = 18000L)
                )
            )
            coEvery { expenseRepository.getExpenseById(expenseId) } returns expense
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs
            coEvery { cashWithdrawalRepository.refundTranche(any(), any()) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then - Should refund both tranches
            coVerify { cashWithdrawalRepository.refundTranche("w-1", 5000L) }
            coVerify { cashWithdrawalRepository.refundTranche("w-2", 18000L) }
            coVerify { expenseRepository.deleteExpense(groupId, expenseId) }
        }

        @Test
        fun `does not refund when expense has no cash tranches`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            val expense = Expense(
                id = expenseId,
                groupId = groupId,
                title = "Dinner",
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5000L,
                groupCurrency = "EUR",
                paymentMethod = PaymentMethod.CREDIT_CARD,
                cashTranches = emptyList()
            )
            coEvery { expenseRepository.getExpenseById(expenseId) } returns expense
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then - No refund calls
            coVerify(exactly = 0) { cashWithdrawalRepository.refundTranche(any(), any()) }
            coVerify { expenseRepository.deleteExpense(groupId, expenseId) }
        }

        @Test
        fun `does not refund when expense not found`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            coEvery { expenseRepository.getExpenseById(expenseId) } returns null
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then - No refund calls
            coVerify(exactly = 0) { cashWithdrawalRepository.refundTranche(any(), any()) }
            coVerify { expenseRepository.deleteExpense(groupId, expenseId) }
        }
    }

    // ── Linked contribution deletion (out-of-pocket cascade) ──────────────────

    @Nested
    inner class LinkedContributionDeletion {

        @Test
        fun `deletes linked contribution for out-of-pocket expense`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-789"
            val expense = Expense(
                id = expenseId,
                groupId = groupId,
                title = "Museum",
                sourceAmount = 1500L,
                sourceCurrency = "EUR",
                groupAmount = 1500L,
                groupCurrency = "EUR",
                paymentMethod = PaymentMethod.CREDIT_CARD,
                payerType = PayerType.USER,
                payerId = "maria-001"
            )
            coEvery { expenseRepository.getExpenseById(expenseId) } returns expense
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then — cascade-delete must happen before expense deletion
            coVerifyOrder {
                contributionRepository.deleteByLinkedExpenseId(groupId, expenseId)
                expenseRepository.deleteExpense(groupId, expenseId)
            }
        }

        @Test
        fun `calls deleteByLinkedExpenseId for group expense (graceful no-op)`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-456"
            val expense = Expense(
                id = expenseId,
                groupId = groupId,
                title = "Dinner",
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5000L,
                groupCurrency = "EUR",
                paymentMethod = PaymentMethod.CREDIT_CARD,
                payerType = PayerType.GROUP
            )
            coEvery { expenseRepository.getExpenseById(expenseId) } returns expense
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then
            // Called for all expenses — safe no-op when no linked contribution exists
            coVerify(exactly = 1) {
                contributionRepository.deleteByLinkedExpenseId(groupId, expenseId)
            }
        }

        @Test
        fun `handles out-of-pocket cash with tranches - both refund and cascade`() = runTest {
            // Given
            val groupId = "group-123"
            val expenseId = "expense-cash-oop"
            val expense = Expense(
                id = expenseId,
                groupId = groupId,
                title = "Street food",
                sourceAmount = 30000L,
                sourceCurrency = "THB",
                groupAmount = 810L,
                groupCurrency = "EUR",
                paymentMethod = PaymentMethod.CASH,
                payerType = PayerType.USER,
                payerId = "andres-001",
                cashTranches = listOf(
                    CashTranche(withdrawalId = "w-1", amountConsumed = 30000L)
                )
            )
            coEvery { expenseRepository.getExpenseById(expenseId) } returns expense
            coEvery { expenseRepository.deleteExpense(groupId, expenseId) } just Runs
            coEvery { cashWithdrawalRepository.refundTranche(any(), any()) } just Runs

            // When
            useCase(groupId, expenseId)

            // Then — refund, cascade-delete, and expense deletion in correct order
            coVerifyOrder {
                cashWithdrawalRepository.refundTranche("w-1", 30000L)
                contributionRepository.deleteByLinkedExpenseId(groupId, expenseId)
                expenseRepository.deleteExpense(groupId, expenseId)
            }
        }
    }
}
