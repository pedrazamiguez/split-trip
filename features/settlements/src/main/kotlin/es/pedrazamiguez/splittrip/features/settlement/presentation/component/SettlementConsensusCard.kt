package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.AnimatedAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel

@Composable
internal fun SettlementConsensusCard(
    item: SettlementConsensusItemUiModel,
    onConfirm: () -> Unit,
    onDispute: () -> Unit,
    onNudge: () -> Unit,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false
) {
    SectionCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.directionLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.pocketTypeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedAmount(
                formattedAmount = item.formattedAmount,
                shouldAnimate = false,
                style = MaterialTheme.typography.titleLarge
            )
            PassportTag(
                text = item.statusLabel,
                style = item.statusChipStyle
            )
        }

        if (!item.disputeReason.isNullOrBlank()) {
            CaptionText(
                text = item.disputeReason,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (item.canConfirm || item.canDispute || item.canNudge) {
            ConsensusCardActions(
                item = item,
                onConfirm = onConfirm,
                onDispute = onDispute,
                onNudge = onNudge,
                isOffline = isOffline
            )
        }
    }
}
