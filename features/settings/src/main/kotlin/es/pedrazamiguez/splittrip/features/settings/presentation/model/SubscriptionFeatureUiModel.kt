package es.pedrazamiguez.splittrip.features.settings.presentation.model

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText

@Immutable
data class SubscriptionFeatureUiModel(
    val label: UiText,
    val status: SubscriptionFeatureStatus,
    val isHighlighted: Boolean = false
) {
    val isIncluded: Boolean
        get() = status == SubscriptionFeatureStatus.INCLUDED
}
