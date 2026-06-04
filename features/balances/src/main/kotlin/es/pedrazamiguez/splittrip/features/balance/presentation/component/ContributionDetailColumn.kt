package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Link
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
internal fun ContributionDetailColumn(
    contribution: ContributionUiModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = if (contribution.isLinkedContribution) {
                if (contribution.isCurrentUser) {
                    stringResource(R.string.balances_linked_contribution_by_you)
                } else {
                    stringResource(R.string.balances_linked_contribution_by, contribution.displayName)
                }
            } else {
                if (contribution.isCurrentUser) {
                    stringResource(R.string.balances_contribution_by_you)
                } else {
                    stringResource(R.string.balances_contribution_by, contribution.displayName)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        if (contribution.createdByDisplayName != null) {
            SecondaryBodyText(
                text = stringResource(R.string.balances_logged_by, contribution.createdByDisplayName),
                maxLines = Int.MAX_VALUE
            )
        }

        if (contribution.isLinkedContribution) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Link,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                CaptionText(
                    text = stringResource(R.string.balances_linked_contribution_label),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (contribution.scopeLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                Icon(
                    imageVector = when {
                        contribution.isSubunitContribution -> TablerIcons.Outline.Sitemap
                        contribution.isGroupContribution -> TablerIcons.Outline.UsersGroup
                        else -> TablerIcons.Outline.User
                    },
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                CaptionText(
                    text = contribution.scopeLabel.orEmpty(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (contribution.dateText.isNotBlank()) {
            SecondaryBodyText(text = contribution.dateText, maxLines = Int.MAX_VALUE)
        }
    }
}
