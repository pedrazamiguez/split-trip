package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
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
        }
        SyncStatusBadge(syncStatus = withdrawal.syncStatus)
    }
}
