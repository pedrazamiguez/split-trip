package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.profile.presentation.model.ProfileUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: ProfileUiModel? = null,
    val hasError: Boolean = false,
    val linkedProviders: ImmutableList<AuthProviderType> = persistentListOf(),
    val isLinking: Boolean = false,
    val showLinkEmailDialog: Boolean = false,
    val linkPasswordInput: String = "",
    val linkConfirmPasswordInput: String = "",
    val linkPasswordError: UiText? = null
)
