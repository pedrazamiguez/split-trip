package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class EditGroupUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val groupName: String = "",
    val groupDescription: String = "",
    val selectedCurrency: CurrencyUiModel? = null,
    val extraCurrencies: ImmutableList<CurrencyUiModel> = persistentListOf(),
    val availableCurrencies: ImmutableList<CurrencyUiModel> = persistentListOf(),
    val imageUrl: String? = null,
    val localGroupImagePath: String? = null,
    val isNameValid: Boolean = true,
    val isLoadingCurrencies: Boolean = false,
    val isCoverUploadEnabled: Boolean = false,
    val showImageSourceSheet: Boolean = false
)
