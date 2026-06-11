package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileUiModel? = null,
    val hasError: Boolean = false
)
