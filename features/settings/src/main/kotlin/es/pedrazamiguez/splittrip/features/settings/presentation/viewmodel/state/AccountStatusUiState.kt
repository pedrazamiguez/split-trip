package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class AccountStatusUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val joinDateText: String = "",
    val linkedProviders: ImmutableList<AuthProviderType> = persistentListOf(),
    val isLinking: Boolean = false,
    val showLinkEmailDialog: Boolean = false,
    val linkPasswordInput: String = "",
    val linkConfirmPasswordInput: String = "",
    val linkPasswordError: UiText? = null,
    val showDeleteAccountDialog: Boolean = false
)
