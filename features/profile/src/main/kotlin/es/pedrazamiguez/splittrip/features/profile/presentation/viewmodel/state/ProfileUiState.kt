package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileUiModel? = null,
    val hasError: Boolean = false,
    val linkedProviders: List<AuthProviderType> = emptyList(),
    val isLinking: Boolean = false,
    val showLinkEmailDialog: Boolean = false,
    val linkPasswordInput: String = "",
    val linkConfirmPasswordInput: String = "",
    val linkPasswordError: UiText? = null
)
