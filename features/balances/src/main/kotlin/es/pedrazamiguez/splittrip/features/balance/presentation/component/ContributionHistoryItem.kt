package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCardPay
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContributionHistoryItem(
    contribution: ContributionUiModel,
    modifier: Modifier = Modifier,
    /** Null for linked (auto-generated) contributions — they must not be deletable. */
    onLongClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val isLinked = contribution.isLinkedContribution

    val cardModifier = if (onLongClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = {},
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    } else {
        Modifier.fillMaxWidth()
    }

    Box(modifier = modifier) {
        FlatCard(modifier = cardModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.Default, vertical = MaterialTheme.spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                Icon(
                    imageVector = if (isLinked) TablerIcons.Outline.CreditCardPay else TablerIcons.Outline.Wallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                ContributionDetailColumn(contribution = contribution, modifier = Modifier.weight(1f))
                Text(
                    text = "+${contribution.formattedAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        SyncStatusBadge(syncStatus = contribution.syncStatus)
    }
}
