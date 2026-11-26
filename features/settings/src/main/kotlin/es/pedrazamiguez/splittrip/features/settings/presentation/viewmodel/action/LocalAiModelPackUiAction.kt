package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface LocalAiModelPackUiAction {
    data class ShowTopPill(val message: UiText) : LocalAiModelPackUiAction
    data object RequestUpgrade : LocalAiModelPackUiAction
}
