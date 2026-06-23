package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateGroupUiState(
    val isLoading: Boolean = false,
    val isLoadingCurrencies: Boolean = false,
    val groupName: String = "",
    val groupDescription: String = "",
    val groupMembers: ImmutableList<String> = persistentListOf(),

    // Currency selection
    val availableCurrencies: ImmutableList<CurrencyUiModel> = persistentListOf(),
    val selectedCurrency: CurrencyUiModel? = null,
    val extraCurrencies: ImmutableList<CurrencyUiModel> = persistentListOf(),

    // Member search
    val memberSearchResults: ImmutableList<User> = persistentListOf(),
    val selectedMembers: ImmutableList<User> = persistentListOf(),
    val isSearchingMembers: Boolean = false,

    // Errors
    val error: UiText? = null,
    val isNameValid: Boolean = true,

    // ── Wizard ──────────────────────────────────────────────────────────
    val currentStep: CreateGroupStep = CreateGroupStep.INFO,
    val localGroupImagePath: String? = null,
    val showImageSourceSheet: Boolean = false,
    val isCoverUploadEnabled: Boolean = true
) {
    val steps: List<CreateGroupStep>
        get() = if (selectedMembers.any { it.isPending }) {
            CreateGroupStep.entries
        } else {
            CreateGroupStep.entries.filter { it != CreateGroupStep.UNREGISTERED_NAMES }
        }

    val currentStepIndex: Int
        get() = steps.indexOf(currentStep).coerceAtLeast(0)

    val canGoNext: Boolean
        get() = currentStepIndex < steps.lastIndex

    val isOnReviewStep: Boolean
        get() = currentStep == CreateGroupStep.REVIEW

    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            CreateGroupStep.INFO -> groupName.isNotBlank() && isNameValid
            CreateGroupStep.CURRENCY -> selectedCurrency != null
            CreateGroupStep.MEMBERS -> true
            CreateGroupStep.UNREGISTERED_NAMES -> true
            CreateGroupStep.IMAGE -> true
            CreateGroupStep.REVIEW -> groupName.isNotBlank() && isNameValid && selectedCurrency != null
        }
}
