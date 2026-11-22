package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.enums.PayerType

sealed interface AddContributionUiEvent {
    data class UpdateAmount(val amount: String) : AddContributionUiEvent
    data class ContributionScopeSelected(
        val scope: PayerType,
        val subunitId: String? = null
    ) : AddContributionUiEvent
    data class MemberSelected(val userId: String) : AddContributionUiEvent
    data class ContributionDateChanged(val dateMillis: Long) : AddContributionUiEvent
    data object Submit : AddContributionUiEvent
    data object NextStep : AddContributionUiEvent
    data object PreviousStep : AddContributionUiEvent
    data object CloseWizard : AddContributionUiEvent

    /** Jumps directly to a previously completed step by its zero-based index. */
    data class JumpToStep(val stepIndex: Int) : AddContributionUiEvent

    data class ToggleStepInfoSheet(val visible: Boolean) : AddContributionUiEvent
}
