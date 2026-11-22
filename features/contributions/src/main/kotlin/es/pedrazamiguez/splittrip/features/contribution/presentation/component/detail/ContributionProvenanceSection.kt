package es.pedrazamiguez.splittrip.features.contribution.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.FormerMemberTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusIndicator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.features.contribution.presentation.model.ContributionDetailUiModel

@Composable
internal fun ContributionProvenanceSection(
    contribution: ContributionDetailUiModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.ExtraSmall,
                vertical = MaterialTheme.spacing.Small
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
            if (contribution.createdByText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                ) {
                    CaptionText(
                        text = contribution.createdByText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (contribution.creatorDisplay is MemberDisplay.Former) {
                        FormerMemberTag()
                    }
                }
            }
            if (contribution.createdAtText.isNotBlank()) {
                CaptionText(
                    text = contribution.createdAtText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SyncStatusIndicator(syncStatus = contribution.syncStatus)
    }
}
