package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedCombinedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BasketUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCardPay
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel

@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContributionHistoryItem(
    contribution: ContributionUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    /** Null for linked (auto-generated) contributions — they must not be deletable. */
    onLongClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val isFormer = contribution.memberDisplay is MemberDisplay.Former
    val itemModifier = if (isFormer) modifier.alpha(0.6f) else modifier

    val cardModifier = if (onClick != null || onLongClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .debouncedCombinedClickable(
                enableSpringPress = true,
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick?.let { action ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        action()
                    }
                }
            )
    } else {
        Modifier.fillMaxWidth()
    }

    Box(modifier = itemModifier) {
        FlatCard(modifier = cardModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.Default, vertical = MaterialTheme.spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                Icon(
                    imageVector = resolveContributionIcon(
                        isSettlement = contribution.isSettlementContribution,
                        isLinked = contribution.isLinkedContribution
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                ContributionDetailColumn(contribution = contribution, modifier = Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+${contribution.formattedAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (contribution.isForeignCurrency && contribution.formattedEquivalentAmount.isNotBlank()) {
                        CaptionText(
                            text = "≈ +${contribution.formattedEquivalentAmount}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        SyncStatusBadge(syncStatus = contribution.syncStatus)
    }
}

private fun resolveContributionIcon(isSettlement: Boolean, isLinked: Boolean): ImageVector =
    when {
        isSettlement -> TablerIcons.Outline.BasketUp
        isLinked -> TablerIcons.Outline.CreditCardPay
        else -> TablerIcons.Outline.Wallet
    }
