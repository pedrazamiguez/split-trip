package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CashWithdrawalHistoryItem(
    withdrawal: CashWithdrawalUiModel,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.CashBanknote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))
                WithdrawalDetailColumn(withdrawal = withdrawal, modifier = Modifier.weight(1f))
                WithdrawalAmountColumn(withdrawal = withdrawal)
            }
        }
        SyncStatusBadge(syncStatus = withdrawal.syncStatus)
    }
}

@Composable
private fun WithdrawalDetailColumn(withdrawal: CashWithdrawalUiModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        WithdrawalPrimaryLabel(withdrawal)
        WithdrawalTitleSubtitle(withdrawal.title)
        WithdrawalLoggedByLine(withdrawal.createdByDisplayName)
        WithdrawalScopeBadge(withdrawal)
        WithdrawalDateLine(withdrawal.dateText)
    }
}

@Composable
private fun WithdrawalPrimaryLabel(withdrawal: CashWithdrawalUiModel) {
    Text(
        text = if (withdrawal.isCurrentUser) {
            stringResource(R.string.balances_cash_withdrawal_by_you)
        } else {
            stringResource(R.string.balances_cash_withdrawal_by, withdrawal.displayName)
        },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun WithdrawalTitleSubtitle(title: String?) {
    if (!title.isNullOrBlank()) {
        SecondaryBodyText(text = title)
    }
}

@Composable
private fun WithdrawalLoggedByLine(createdByDisplayName: String?) {
    if (createdByDisplayName != null) {
        SecondaryBodyText(
            text = stringResource(R.string.balances_logged_by, createdByDisplayName),
            maxLines = Int.MAX_VALUE
        )
    }
}

@Composable
private fun WithdrawalScopeBadge(withdrawal: CashWithdrawalUiModel) {
    if (withdrawal.scopeLabel != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
        ) {
            Icon(
                imageVector = when {
                    withdrawal.isSubunitWithdrawal -> TablerIcons.Outline.Sitemap
                    withdrawal.isGroupWithdrawal -> TablerIcons.Outline.UsersGroup
                    else -> TablerIcons.Outline.User
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            CaptionText(
                text = withdrawal.scopeLabel,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun WithdrawalDateLine(dateText: String) {
    if (dateText.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        SecondaryBodyText(text = dateText, maxLines = Int.MAX_VALUE)
    }
}

@Composable
private fun WithdrawalAmountColumn(withdrawal: CashWithdrawalUiModel) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = withdrawal.formattedAmount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary
        )
        if (withdrawal.isForeignCurrency && withdrawal.formattedDeducted.isNotBlank()) {
            BodyText(
                text = withdrawal.formattedDeducted,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
