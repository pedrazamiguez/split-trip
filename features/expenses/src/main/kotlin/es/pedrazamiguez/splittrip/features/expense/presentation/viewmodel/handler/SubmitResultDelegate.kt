package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Stateless delegate that handles the post-submit result processing for expenses.
 *
 * Extracted from [SubmitEventHandler] to reduce cognitive complexity.
 * Handles the success/failure branching after [AddExpenseUseCase] completes,
 * including saving last-used preferences on success and emitting error actions
 * on failure (with special handling for [InsufficientCashException]).
 */
class SubmitResultDelegate(
    private val saveLastUsedPreferences: SaveLastUsedPreferencesBundle,
    private val formattingHelper: FormattingHelper
) {

    /**
     * Handles a successful expense submission: persists user preferences
     * for currency, payment method, and category, then invokes [onSuccess].
     */
    suspend fun handleSuccess(
        uiState: MutableStateFlow<AddExpenseUiState>,
        groupId: String,
        onSuccess: () -> Unit
    ) {
        uiState.value.selectedCurrency?.code?.let { code ->
            runCatching { saveLastUsedPreferences.setGroupLastUsedCurrencyUseCase(groupId, code) }
        }
        uiState.value.selectedPaymentMethod?.id?.let { id ->
            runCatching { saveLastUsedPreferences.setGroupLastUsedPaymentMethodUseCase(groupId, id) }
        }
        uiState.value.selectedCategory?.id?.let { id ->
            runCatching { saveLastUsedPreferences.setGroupLastUsedCategoryUseCase(groupId, id) }
        }
        uiState.update { it.copy(isLoading = false) }
        onSuccess()
    }

    /**
     * Handles a failed expense submission: clears loading state, then emits
     * an appropriate error action via [actionsFlow].
     *
     * For [InsufficientCashException]:
     * - If the tranche preview **already** flagged insufficient cash before the user submitted
     *   ([AddExpenseUiState.isInsufficientCash] == true), the user had a genuine cash shortage
     *   and we show the detailed "not enough cash" message with required/available amounts.
     * - If the preview showed available cash but the save failed, another group member consumed
     *   the cash concurrently (race condition). We emit [AddExpenseUiAction.ShowCashConflictResolution]
     *   so the Feature can show the conflict-resolution bottom sheet and refresh the tranche preview.
     *
     * For [CashConflictException]:
     * - Phase 2: Firestore transaction detected a concurrent modification. Same UX as
     *   Phase 1 race detection, but available amount is unknown so amounts are null.
     *
     * For all other exceptions, emits a generic failure message.
     */
    suspend fun handleFailure(
        error: Throwable,
        uiState: MutableStateFlow<AddExpenseUiState>,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        currentState: AddExpenseUiState
    ) {
        uiState.update { it.copy(isLoading = false, error = null) }

        when (error) {
            is InsufficientCashException -> {
                if (currentState.isInsufficientCash) {
                    // Preview already showed insufficient cash — genuine shortage, not a race.
                    emitInsufficientCashError(error, actionsFlow, currentState)
                } else {
                    // Preview showed available cash — concurrent write raced this submit.
                    emitCashConflictResolution(error.availableCents, actionsFlow, currentState)
                }
            }
            is CashConflictException -> {
                // Phase 2: Firestore transaction detected a concurrent modification.
                // Available amount is unknown at this point; null signals the sheet
                // to omit the "Use remaining cash" option.
                emitCashConflictResolution(null, actionsFlow, currentState)
            }
            else -> actionsFlow.emit(
                AddExpenseUiAction.ShowError(
                    UiText.StringResource(R.string.expense_error_addition_failed)
                )
            )
        }
    }

    /**
     * Emits [AddExpenseUiAction.ShowError] with a detailed "not enough cash" message,
     * showing the required and available amounts in the expense's source currency.
     *
     * Used when the tranche preview **already** flagged insufficient cash before submit
     * (i.e., [AddExpenseUiState.isInsufficientCash] was true at submit time).
     */
    internal suspend fun emitInsufficientCashError(
        error: InsufficientCashException,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        currentState: AddExpenseUiState
    ) {
        val cashCurrency = currentState.selectedCurrency
        if (cashCurrency != null) {
            val required = formattingHelper.formatCentsWithCurrency(error.requiredCents, cashCurrency.code)
            val available = formattingHelper.formatCentsWithCurrency(error.availableCents, cashCurrency.code)
            actionsFlow.emit(
                AddExpenseUiAction.ShowError(
                    UiText.StringResource(R.string.expense_error_insufficient_cash, required, available)
                )
            )
        } else {
            actionsFlow.emit(
                AddExpenseUiAction.ShowError(
                    UiText.StringResource(R.string.expense_error_addition_failed)
                )
            )
        }
    }

    /**
     * Emits [AddExpenseUiAction.ShowCashConflictResolution] to trigger the guided
     * conflict-resolution bottom sheet.
     *
     * Pre-formats [availableCents] into:
     * - [AddExpenseUiAction.ShowCashConflictResolution.availableAmountForInput]: a plain
     *   decimal string (e.g. "30.00" or "30,00") suitable for the source-amount input field.
     * - [AddExpenseUiAction.ShowCashConflictResolution.availableAmountDisplay]: a
     *   currency-symbol string (e.g. "€30.00") for the "Use remaining cash" CTA label.
     *
     * Both fields are `null` when [availableCents] is null (i.e. [CashConflictException]
     * path where the available amount is unknown at emit time).
     */
    internal suspend fun emitCashConflictResolution(
        availableCents: Long?,
        actionsFlow: MutableSharedFlow<AddExpenseUiAction>,
        currentState: AddExpenseUiState
    ) {
        val currency = currentState.selectedCurrency
        val amountForInput: String?
        val amountDisplay: String?
        if (availableCents != null && availableCents >= 0 && currency != null) {
            amountForInput = formattingHelper.formatCentsValue(availableCents, currency.decimalDigits)
            amountDisplay = formattingHelper.formatCentsWithCurrency(availableCents, currency.code)
        } else {
            amountForInput = null
            amountDisplay = null
        }
        actionsFlow.emit(
            AddExpenseUiAction.ShowCashConflictResolution(
                availableAmountForInput = amountForInput,
                availableAmountDisplay = amountDisplay
            )
        )
    }
}
