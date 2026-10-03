package es.pedrazamiguez.splittrip.features.contribution.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail.ContributionHeroSection
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail.ContributionProvenanceSection
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail.ContributionScopeSection
import es.pedrazamiguez.splittrip.features.contribution.presentation.model.ContributionDetailUiModel

@Composable
internal fun ContributionDetailContent(
    contribution: ContributionDetailUiModel,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    topPadding: Dp = LocalTopPadding.current
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                PaddingValues(
                    start = MaterialTheme.spacing.Default,
                    top = MaterialTheme.spacing.Default + topPadding,
                    end = MaterialTheme.spacing.Default,
                    bottom = MaterialTheme.spacing.Default + bottomPadding
                )
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
    ) {
        ContributionHeroSection(contribution = contribution)
        ContributionScopeSection(contribution = contribution)
        ContributionProvenanceSection(contribution = contribution)
    }
}
