package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowancePerkUiModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.AllowanceQuotaUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PlanAllowancesUiState(
    val isLoading: Boolean = true,
    val isProTier: Boolean = false,
    val isGatingEnabled: Boolean = true,
    val tierTitle: UiText = UiText.DynamicString(""),
    val tierSubtitle: UiText = UiText.DynamicString(""),
    val tierBadge: UiText = UiText.DynamicString(""),
    val quotas: ImmutableList<AllowanceQuotaUiModel> = persistentListOf(),
    val perks: ImmutableList<AllowancePerkUiModel> = persistentListOf(),
    val showUpgradeCta: Boolean = false
)
