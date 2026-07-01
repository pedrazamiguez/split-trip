package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.User
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateEditGroupUiState(
    val isLoading: Boolean = false,
    val isLoadingCurrencies: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val groupId: String? = null,
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

    // Wizard
    val currentStep: CreateEditGroupStep = CreateEditGroupStep.INFO,
    val imageUrl: String? = null,
    val localGroupImagePath: String? = null,
    val showImageSourceSheet: Boolean = false,
    val isCoverUploadEnabled: Boolean = true
) {
    val steps: List<CreateEditGroupStep>
        get() = if (isEditMode) {
            if (selectedMembers.any { it.isPending }) {
                listOf(
                    CreateEditGroupStep.INFO,
                    CreateEditGroupStep.CURRENCY,
                    CreateEditGroupStep.MEMBERS,
                    CreateEditGroupStep.UNREGISTERED_NAMES,
                    CreateEditGroupStep.IMAGE,
                    CreateEditGroupStep.REVIEW
                )
            } else {
                listOf(
                    CreateEditGroupStep.INFO,
                    CreateEditGroupStep.CURRENCY,
                    CreateEditGroupStep.MEMBERS,
                    CreateEditGroupStep.IMAGE,
                    CreateEditGroupStep.REVIEW
                )
            }
        } else if (selectedMembers.any { it.isPending }) {
            CreateEditGroupStep.entries
        } else {
            CreateEditGroupStep.entries.filter { it != CreateEditGroupStep.UNREGISTERED_NAMES }
        }

    val currentStepIndex: Int
        get() = steps.indexOf(currentStep).coerceAtLeast(0)

    val canGoNext: Boolean
        get() = currentStepIndex < steps.lastIndex

    val isOnReviewStep: Boolean
        get() = currentStep == CreateEditGroupStep.REVIEW

    val isCurrentStepValid: Boolean
        get() = when (currentStep) {
            CreateEditGroupStep.INFO -> groupName.isNotBlank() && isNameValid
            CreateEditGroupStep.CURRENCY -> selectedCurrency != null
            CreateEditGroupStep.MEMBERS -> true
            CreateEditGroupStep.UNREGISTERED_NAMES -> true
            CreateEditGroupStep.IMAGE -> true
            CreateEditGroupStep.REVIEW -> groupName.isNotBlank() && isNameValid && selectedCurrency != null
        }
}
