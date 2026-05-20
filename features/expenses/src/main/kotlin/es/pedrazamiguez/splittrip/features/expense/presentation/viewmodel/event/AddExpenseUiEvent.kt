package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PayerType

sealed interface AddExpenseUiEvent {
    // ...existing events...
    data class LoadGroupConfig(val groupId: String?) : AddExpenseUiEvent
    data class RetryLoadConfig(val groupId: String?) : AddExpenseUiEvent
    data class TitleChanged(val title: String) : AddExpenseUiEvent
    data class SourceAmountChanged(val amount: String) : AddExpenseUiEvent
    data class CurrencySelected(val currencyCode: String) : AddExpenseUiEvent
    data class PaymentMethodSelected(val methodId: String) : AddExpenseUiEvent
    data class FundingSourceSelected(val fundingSourceId: String) : AddExpenseUiEvent
    data class ContributionScopeSelected(val scope: PayerType, val subunitId: String?) : AddExpenseUiEvent

    /**
     * Fired when the user selects a specific withdrawal pool from the pool-selection widget
     * in the Exchange Rate step. Only shown when multiple pools have available funds.
     *
     * @param scope     The selected pool's scope (GROUP, USER, or SUBUNIT).
     * @param subunitId The subunit ID when [scope] is SUBUNIT; null otherwise.
     */
    data class WithdrawalPoolSelected(val scope: PayerType, val scopeOwnerId: String?) : AddExpenseUiEvent
    data class ExchangeRateChanged(val rate: String) : AddExpenseUiEvent
    data class GroupAmountChanged(val amount: String) : AddExpenseUiEvent
    data class CategorySelected(val categoryId: String) : AddExpenseUiEvent
    data class VendorChanged(val vendor: String) : AddExpenseUiEvent
    data class NotesChanged(val notes: String) : AddExpenseUiEvent
    data class PaymentStatusSelected(val statusId: String) : AddExpenseUiEvent
    data class DueDateSelected(val dateMillis: Long) : AddExpenseUiEvent
    data class ReceiptImageSelected(val uri: String) : AddExpenseUiEvent
    data object RemoveReceiptImage : AddExpenseUiEvent

    /**
     * Signals the Feature layer to show the receipt source selection sheet
     * (camera / gallery / document). Handled in [AddExpenseFeature] before
     * reaching the ViewModel so the ViewModel stays context-free.
     */
    data object RequestPickerSource : AddExpenseUiEvent
    data class SubmitAddExpense(val groupId: String?) : AddExpenseUiEvent

    // Split events
    data class SplitTypeChanged(val splitTypeId: String) : AddExpenseUiEvent
    data class SplitAmountChanged(val userId: String, val amount: String) : AddExpenseUiEvent
    data class SplitPercentageChanged(val userId: String, val percentage: String) : AddExpenseUiEvent
    data class SplitExcludedToggled(val userId: String) : AddExpenseUiEvent
    data class SplitShareLockToggled(val userId: String) : AddExpenseUiEvent

    // Subunit split events
    data object SubunitModeToggled : AddExpenseUiEvent
    data class EntitySplitExcludedToggled(val entityId: String) : AddExpenseUiEvent
    data class EntitySplitAmountChanged(val entityId: String, val amount: String) : AddExpenseUiEvent
    data class EntitySplitPercentageChanged(val entityId: String, val percentage: String) : AddExpenseUiEvent
    data class EntityShareLockToggled(val entityId: String) : AddExpenseUiEvent
    data class IntraSubunitSplitTypeChanged(val subunitId: String, val splitTypeId: String) : AddExpenseUiEvent
    data class IntraSubunitAmountChanged(
        val subunitId: String,
        val userId: String,
        val amount: String
    ) : AddExpenseUiEvent
    data class IntraSubunitPercentageChanged(
        val subunitId: String,
        val userId: String,
        val percentage: String
    ) : AddExpenseUiEvent
    data class IntraSubunitShareLockToggled(
        val subunitId: String,
        val userId: String
    ) : AddExpenseUiEvent
    data class EntityAccordionToggled(val entityId: String) : AddExpenseUiEvent

    // Add-on events
    /** Adds a new add-on of the given type to the list. */
    data class AddOnAdded(val type: AddOnType) : AddExpenseUiEvent

    /** Removes the add-on with the given id. */
    data class AddOnRemoved(val addOnId: String) : AddExpenseUiEvent

    /** Changes the type (TIP, FEE, DISCOUNT, SURCHARGE) of an existing add-on. */
    data class AddOnTypeChanged(val addOnId: String, val type: AddOnType) : AddExpenseUiEvent

    /** Changes the mode (ON_TOP, INCLUDED) of an existing add-on. */
    data class AddOnModeChanged(val addOnId: String, val mode: AddOnMode) : AddExpenseUiEvent

    /** Changes the value type (EXACT, PERCENTAGE) of an existing add-on. */
    data class AddOnValueTypeChanged(
        val addOnId: String,
        val valueType: AddOnValueType
    ) : AddExpenseUiEvent

    /** Updates the amount input for an add-on. */
    data class AddOnAmountChanged(val addOnId: String, val amount: String) : AddExpenseUiEvent

    /** Changes the currency of an add-on. */
    data class AddOnCurrencySelected(
        val addOnId: String,
        val currencyCode: String
    ) : AddExpenseUiEvent

    /** Changes the payment method of an add-on. */
    data class AddOnPaymentMethodSelected(
        val addOnId: String,
        val methodId: String
    ) : AddExpenseUiEvent

    /** Updates the free-text description of an add-on. */
    data class AddOnDescriptionChanged(
        val addOnId: String,
        val description: String
    ) : AddExpenseUiEvent

    /** Updates the per-add-on exchange rate (manual override). */
    data class AddOnExchangeRateChanged(
        val addOnId: String,
        val rate: String
    ) : AddExpenseUiEvent

    /** Updates the per-add-on converted group amount (reverse calculation). */
    data class AddOnGroupAmountChanged(
        val addOnId: String,
        val amount: String
    ) : AddExpenseUiEvent

    /** Toggles the add-ons section expansion. */
    data object AddOnsSectionToggled : AddExpenseUiEvent

    // ── Wizard Navigation ───────────────────────────────────────────────
    data object NextStep : AddExpenseUiEvent
    data object PreviousStep : AddExpenseUiEvent

    /** Jumps directly from the current optional step to the REVIEW step. */
    data object JumpToReview : AddExpenseUiEvent

    /** Jumps directly to a previously completed step by its zero-based index. */
    data class JumpToStep(val stepIndex: Int) : AddExpenseUiEvent

    /**
     * Fired from the conflict-resolution sheet when the user taps "Use remaining cash".
     *
     * [amount] is a pre-formatted, locale-aware decimal string (e.g. "30.00" or "30,00")
     * already suitable for the source-amount input field. The ViewModel routes this directly
     * to [FormEventHandler.handleSourceAmountChanged] so the full recalculation chain
     * (FIFO preview, split recalculation, add-on totals) fires automatically.
     */
    data class ResolutionAmountSelected(val amount: String) : AddExpenseUiEvent
}
