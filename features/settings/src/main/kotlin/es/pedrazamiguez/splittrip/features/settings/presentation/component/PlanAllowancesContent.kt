package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.PlanAllowancesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.PlanAllowancesUiState

@Composable
internal fun PlanAllowancesContent(
    uiState: PlanAllowancesUiState,
    onEvent: (PlanAllowancesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomPadding.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
            .padding(
                top = MaterialTheme.spacing.ExtraLarge,
                bottom = MaterialTheme.spacing.ExtraLarge + bottomPadding
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraLarge)
    ) {
        PlanAllowancesTierCard(
            isProTier = uiState.isProTier,
            isGatingEnabled = uiState.isGatingEnabled,
            title = uiState.tierTitle,
            subtitle = uiState.tierSubtitle,
            badge = uiState.tierBadge
        )

        SectionCard(title = stringResource(R.string.plan_allowances_section_usage)) {
            uiState.quotas.forEach { quota ->
                PlanAllowanceQuotaItem(quota = quota)
            }
        }

        SectionCard(title = stringResource(R.string.plan_allowances_section_allowances)) {
            uiState.perks.forEach { perk ->
                PlanAllowancePerkRow(perk = perk)
            }
        }

        if (uiState.showUpgradeCta) {
            PlanAllowancesUpgradeCard(
                description = uiState.upgradeDescription,
                onUpgradeClick = { onEvent(PlanAllowancesUiEvent.RequestUpgrade) }
            )
        }
    }
}
