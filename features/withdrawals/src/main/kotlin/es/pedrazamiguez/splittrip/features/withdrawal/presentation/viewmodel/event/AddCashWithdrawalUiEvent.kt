package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.enums.PayerType

sealed interface AddCashWithdrawalUiEvent {
    data class LoadGroupConfig(val groupId: String?) : AddCashWithdrawalUiEvent
    data class RetryLoadConfig(val groupId: String?) : AddCashWithdrawalUiEvent
    data class CurrencySelected(val currencyCode: String) : AddCashWithdrawalUiEvent
    data class WithdrawalAmountChanged(val amount: String) : AddCashWithdrawalUiEvent
    data class DeductedAmountChanged(val amount: String) : AddCashWithdrawalUiEvent
    data class ExchangeRateChanged(val rate: String) : AddCashWithdrawalUiEvent
    data class WithdrawalScopeSelected(val scope: PayerType, val subunitId: String? = null) : AddCashWithdrawalUiEvent
    data class MemberSelected(val userId: String) : AddCashWithdrawalUiEvent
    data class TitleChanged(val title: String) : AddCashWithdrawalUiEvent
    data class NotesChanged(val notes: String) : AddCashWithdrawalUiEvent
    data class SubmitWithdrawal(val groupId: String?) : AddCashWithdrawalUiEvent

    // ATM Fee events
    data class FeeToggled(val hasFee: Boolean) : AddCashWithdrawalUiEvent
    data class FeeAmountChanged(val amount: String) : AddCashWithdrawalUiEvent
    data class FeeCurrencySelected(val currencyCode: String) : AddCashWithdrawalUiEvent
    data class FeeExchangeRateChanged(val rate: String) : AddCashWithdrawalUiEvent
    data class FeeConvertedAmountChanged(val amount: String) : AddCashWithdrawalUiEvent

    // ── Wizard Navigation ───────────────────────────────────────────────
    data object NextStep : AddCashWithdrawalUiEvent
    data object PreviousStep : AddCashWithdrawalUiEvent
    data object CloseWizard : AddCashWithdrawalUiEvent

    /** Jumps directly from the current optional step to the REVIEW step. */
    data object JumpToReview : AddCashWithdrawalUiEvent

    /** Jumps directly to a previously completed step by its zero-based index. */
    data class JumpToStep(val stepIndex: Int) : AddCashWithdrawalUiEvent

    data class ToggleStepInfoSheet(val visible: Boolean) : AddCashWithdrawalUiEvent
}
