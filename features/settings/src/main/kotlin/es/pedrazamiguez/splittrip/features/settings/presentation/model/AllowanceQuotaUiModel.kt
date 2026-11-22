package es.pedrazamiguez.splittrip.features.settings.presentation.model

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText

@Immutable
data class AllowanceQuotaUiModel(
    val type: AllowanceQuotaType,
    val title: UiText,
    val subtitle: UiText,
    val usageText: UiText,
    val statusBadge: UiText,
    val status: AllowanceUsageStatus,
    val progressFraction: Float,
    val showProgressBar: Boolean
)
