package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface NotificationPreferencesUiAction {
    data class ShowTopPill(val message: UiText) : NotificationPreferencesUiAction
}
