package es.pedrazamiguez.splittrip.features.settings.presentation.model

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText

@Immutable
data class AllowancePerkUiModel(
    val type: AllowancePerkType,
    val title: UiText,
    val detail: UiText,
    val isIncluded: Boolean
)
