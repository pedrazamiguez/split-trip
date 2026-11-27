package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle

sealed interface LocalAiModelPackUiAction {
    data class ShowTopPill(
        val message: UiText,
        val style: TopPillStyle = TopPillStyle.NEUTRAL
    ) : LocalAiModelPackUiAction
    data object RequestUpgrade : LocalAiModelPackUiAction
}
