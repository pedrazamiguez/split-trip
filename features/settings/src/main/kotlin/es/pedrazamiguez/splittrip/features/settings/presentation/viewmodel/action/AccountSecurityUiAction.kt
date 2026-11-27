package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle

sealed interface AccountSecurityUiAction {
    data class ShowTopPill(
        val message: UiText,
        val style: TopPillStyle = TopPillStyle.NEUTRAL
    ) : AccountSecurityUiAction
    data class NavigateToRoute(val route: String) : AccountSecurityUiAction
    data object NavigateBack : AccountSecurityUiAction
    data object RequestBiometricConfirmation : AccountSecurityUiAction
}
