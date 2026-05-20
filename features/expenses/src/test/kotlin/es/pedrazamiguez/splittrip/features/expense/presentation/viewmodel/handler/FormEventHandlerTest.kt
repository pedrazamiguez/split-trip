package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.usecase.expense.AttachReceiptUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormEventHandlerTest {

    private lateinit var handler: FormEventHandler
    private lateinit var addExpenseUiMapper: AddExpenseUiMapper
    private lateinit var attachReceiptUseCase: AttachReceiptUseCase
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>
    private val capturedPostActions = mutableListOf<FormPostAction>()

    private val cashMethod = PaymentMethodUiModel(id = PaymentMethod.CASH.name, displayText = "Cash")
    private val cardMethod = PaymentMethodUiModel(id = PaymentMethod.CREDIT_CARD.name, displayText = "Credit Card")
    private val groupSource = FundingSourceUiModel(id = PayerType.GROUP.name, displayText = "Group Pocket")
    private val userSource = FundingSourceUiModel(id = PayerType.USER.name, displayText = "My Money")
    private val finishedStatus = PaymentStatusUiModel(id = PaymentStatus.FINISHED.name, displayText = "Finished")
    private val scheduledStatus = PaymentStatusUiModel(id = PaymentStatus.SCHEDULED.name, displayText = "Scheduled")
    private val foodCategory = CategoryUiModel(id = "FOOD", displayText = "Food")
    private val otherCategory = CategoryUiModel(id = "OTHER", displayText = "Other")

    @BeforeEach
    fun setUp() {
        addExpenseUiMapper = mockk(relaxed = true)
        attachReceiptUseCase = mockk(relaxed = true)
        capturedPostActions.clear()

        handler = FormEventHandler(
            addExpenseUiMapper = addExpenseUiMapper,
            attachReceiptUseCase = attachReceiptUseCase
        )
        handler.setFormPostCallback { capturedPostActions.add(it) }

        uiState = MutableStateFlow(
            AddExpenseUiState(
                paymentMethods = persistentListOf(cashMethod, cardMethod),
                fundingSources = persistentListOf(groupSource, userSource),
                availablePaymentStatuses = persistentListOf(finishedStatus, scheduledStatus),
                availableCategories = persistentListOf(foodCategory, otherCategory),
                selectedFundingSource = groupSource
            )
        )
        actions = MutableSharedFlow()
        // UnconfinedTestDispatcher runs coroutines eagerly so no advanceUntilIdle() is needed
        handler.bind(uiState, actions, TestScope(UnconfinedTestDispatcher()))
    }

    @Nested
    inner class TitleChanged {

        @Test
        fun `updates title and clears validation error`() = runTest {
            uiState.value = uiState.value.copy(isTitleValid = false, error = UiText.DynamicString("err"))

            handler.handleTitleChanged("Dinner")

            assertEquals("Dinner", uiState.value.expenseTitle)
            assertTrue(uiState.value.isTitleValid)
            assertNull(uiState.value.error)
        }
    }

    @Nested
    inner class SourceAmountChanged {

        @Test
        fun `updates amount and clears validation error`() = runTest {
            uiState.value = uiState.value.copy(isAmountValid = false, error = UiText.DynamicString("err"))

            handler.handleSourceAmountChanged("42.50")

            assertEquals("42.50", uiState.value.sourceAmount)
            assertTrue(uiState.value.isAmountValid)
            assertNull(uiState.value.error)
        }

        @Test
        fun `blank amount is considered valid (user still typing)`() = runTest {
            handler.handleSourceAmountChanged("")
            assertTrue(uiState.value.isAmountValid)
        }

        @Test
        fun `non-numeric text sets isAmountValid false`() = runTest {
            handler.handleSourceAmountChanged("abc")
            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `zero amount sets isAmountValid false`() = runTest {
            handler.handleSourceAmountChanged("0")
            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `negative amount sets isAmountValid false`() = runTest {
            handler.handleSourceAmountChanged("-10")
            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `emits RecalculateAfterAmount with locked rate`() = runTest {
            uiState.value = uiState.value.copy(isExchangeRateLocked = true)

            handler.handleSourceAmountChanged("100")

            val action = capturedPostActions.single()
            assertTrue(action is FormPostAction.RecalculateAfterAmount)
            assertTrue((action as FormPostAction.RecalculateAfterAmount).isExchangeRateLocked)
        }

        @Test
        fun `emits RecalculateAfterAmount with unlocked rate`() = runTest {
            uiState.value = uiState.value.copy(isExchangeRateLocked = false)

            handler.handleSourceAmountChanged("100")

            val action = capturedPostActions.single()
            assertTrue(action is FormPostAction.RecalculateAfterAmount)
            assertFalse((action as FormPostAction.RecalculateAfterAmount).isExchangeRateLocked)
        }

        @Test
        fun `emits RecalculateAfterAmount with isCash true when CASH is selected`() = runTest {
            uiState.value = uiState.value.copy(selectedPaymentMethod = cashMethod)

            handler.handleSourceAmountChanged("50")

            val action = capturedPostActions.single() as FormPostAction.RecalculateAfterAmount
            assertTrue(action.isCash)
        }

        @Test
        fun `emits RecalculateAfterAmount with isCash false when non-CASH is selected`() = runTest {
            uiState.value = uiState.value.copy(selectedPaymentMethod = cardMethod)

            handler.handleSourceAmountChanged("50")

            val action = capturedPostActions.single() as FormPostAction.RecalculateAfterAmount
            assertFalse(action.isCash)
        }
    }

    @Nested
    inner class PaymentMethodSelected {

        @Test
        fun `selects payment method and emits callback`() = runTest {
            handler.handlePaymentMethodSelected(cardMethod.id)

            assertEquals(cardMethod, uiState.value.selectedPaymentMethod)
            val action = capturedPostActions.single() as FormPostAction.PaymentMethodChanged
            assertFalse(action.isCash)
            assertTrue(action.isGroupPocket) // default selectedFundingSource is GROUP
        }

        @Test
        fun `detects CASH payment method`() = runTest {
            handler.handlePaymentMethodSelected(cashMethod.id)

            assertEquals(cashMethod, uiState.value.selectedPaymentMethod)
            val action = capturedPostActions.single() as FormPostAction.PaymentMethodChanged
            assertTrue(action.isCash)
        }

        @Test
        fun `ignores unknown method ID`() = runTest {
            val before = uiState.value.selectedPaymentMethod

            handler.handlePaymentMethodSelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedPaymentMethod)
            assertTrue(capturedPostActions.isEmpty())
        }

        @Test
        fun `detects non-group funding source`() = runTest {
            uiState.value = uiState.value.copy(selectedFundingSource = userSource)

            handler.handlePaymentMethodSelected(cashMethod.id)

            val action = capturedPostActions.single() as FormPostAction.PaymentMethodChanged
            assertFalse(action.isGroupPocket)
        }
    }

    @Nested
    inner class FundingSourceSelected {

        @Test
        fun `selects GROUP funding source`() = runTest {
            uiState.value = uiState.value.copy(selectedFundingSource = userSource)

            handler.handleFundingSourceSelected(PayerType.GROUP.name)

            assertEquals(groupSource, uiState.value.selectedFundingSource)
            assertNull(uiState.value.fundingSourceHint)
            val action = capturedPostActions.single() as FormPostAction.FundingSourceChanged
            assertTrue(action.isGroupPocket)
        }

        @Test
        fun `selects USER funding source with hint`() = runTest {
            handler.handleFundingSourceSelected(PayerType.USER.name)

            assertEquals(userSource, uiState.value.selectedFundingSource)
            assertTrue(uiState.value.fundingSourceHint is UiText.StringResource)
            val action = capturedPostActions.single() as FormPostAction.FundingSourceChanged
            assertFalse(action.isGroupPocket)
        }

        @Test
        fun `resets contribution scope when switching away from My Money`() = runTest {
            uiState.value = uiState.value.copy(
                selectedFundingSource = userSource,
                contributionScope = PayerType.GROUP,
                selectedContributionSubunitId = "sub-1"
            )

            handler.handleFundingSourceSelected(PayerType.GROUP.name)

            assertEquals(PayerType.USER, uiState.value.contributionScope)
            assertNull(uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `preserves contribution scope when staying on My Money`() = runTest {
            uiState.value = uiState.value.copy(
                selectedFundingSource = userSource,
                contributionScope = PayerType.SUBUNIT,
                selectedContributionSubunitId = "sub-1"
            )

            handler.handleFundingSourceSelected(PayerType.USER.name)

            assertEquals(PayerType.SUBUNIT, uiState.value.contributionScope)
            assertEquals("sub-1", uiState.value.selectedContributionSubunitId)
        }

        @Test
        fun `ignores unknown funding source ID`() = runTest {
            val before = uiState.value.selectedFundingSource

            handler.handleFundingSourceSelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedFundingSource)
            assertTrue(capturedPostActions.isEmpty())
        }
    }

    @Nested
    inner class ContributionScopeSelected {

        @Test
        fun `updates contribution scope and subunit ID`() = runTest {
            handler.handleContributionScopeSelected(PayerType.SUBUNIT, "sub-1")

            assertEquals(PayerType.SUBUNIT, uiState.value.contributionScope)
            assertEquals("sub-1", uiState.value.selectedContributionSubunitId)
        }
    }

    @Nested
    inner class CategorySelected {

        @Test
        fun `selects category`() = runTest {
            handler.handleCategorySelected("FOOD")

            assertEquals(foodCategory, uiState.value.selectedCategory)
        }

        @Test
        fun `ignores unknown category ID`() = runTest {
            val before = uiState.value.selectedCategory

            handler.handleCategorySelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedCategory)
        }
    }

    @Nested
    inner class SimpleFieldUpdates {

        @Test
        fun `updates vendor`() = runTest {
            handler.handleVendorChanged("Starbucks")
            assertEquals("Starbucks", uiState.value.vendor)
        }

        @Test
        fun `updates notes`() = runTest {
            handler.handleNotesChanged("Important note")
            assertEquals("Important note", uiState.value.notes)
        }

        @Test
        fun `sets receipt URI and attachment when use case succeeds`() = runTest {
            val attachment = ReceiptAttachment(
                localUri = "/data/user/0/receipts/abc.webp",
                mimeType = "image/webp",
                capturedAtMillis = 1716000000000L
            )
            coEvery { attachReceiptUseCase("content://image/1") } returns Result.success(attachment)

            handler.handleReceiptImageChanged("content://image/1")

            assertEquals("/data/user/0/receipts/abc.webp", uiState.value.receiptUri)
            assertEquals(attachment, uiState.value.receiptAttachment)
        }

        @Test
        fun `clears receipt URI when uri is null`() = runTest {
            uiState.value = uiState.value.copy(receiptUri = "content://image/1")

            handler.handleReceiptImageChanged(null)

            assertNull(uiState.value.receiptUri)
            assertNull(uiState.value.receiptAttachment)
        }
    }

    @Nested
    inner class PaymentStatusSelected {

        @Test
        fun `shows due date section for SCHEDULED`() = runTest {
            handler.handlePaymentStatusSelected(PaymentStatus.SCHEDULED.name)

            assertEquals(scheduledStatus, uiState.value.selectedPaymentStatus)
            assertTrue(uiState.value.showDueDateSection)
            assertTrue(uiState.value.isDueDateValid)
        }

        @Test
        fun `hides due date section for FINISHED`() = runTest {
            uiState.value = uiState.value.copy(
                showDueDateSection = true,
                dueDateMillis = 12345L,
                formattedDueDate = "2025-01-01"
            )

            handler.handlePaymentStatusSelected(PaymentStatus.FINISHED.name)

            assertEquals(finishedStatus, uiState.value.selectedPaymentStatus)
            assertFalse(uiState.value.showDueDateSection)
            assertNull(uiState.value.dueDateMillis)
            assertEquals("", uiState.value.formattedDueDate)
        }

        @Test
        fun `ignores unknown status ID`() = runTest {
            val before = uiState.value.selectedPaymentStatus

            handler.handlePaymentStatusSelected("NONEXISTENT")

            assertEquals(before, uiState.value.selectedPaymentStatus)
        }
    }

    @Nested
    inner class DueDateSelected {

        @Test
        fun `sets due date millis and formatted date`() = runTest {
            every { addExpenseUiMapper.formatDueDateForDisplay(12345L) } returns "Jan 1, 2025"

            handler.handleDueDateSelected(12345L)

            assertEquals(12345L, uiState.value.dueDateMillis)
            assertEquals("Jan 1, 2025", uiState.value.formattedDueDate)
            assertTrue(uiState.value.isDueDateValid)
        }
    }
}
