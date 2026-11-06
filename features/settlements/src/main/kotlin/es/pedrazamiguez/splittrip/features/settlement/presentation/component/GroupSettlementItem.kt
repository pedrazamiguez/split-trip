package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementRowUiModel

@Composable
internal fun GroupSettlementItem(
    settlement: SettlementRowUiModel,
    onConfirm: () -> Unit,
    onDispute: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PassportTag(
                    text = settlement.statusLabel,
                    style = settlement.statusChipStyle
                )
                PassportTag(
                    text = settlement.pocketTypeLabel,
                    style = PassportTagStyle.NEUTRAL
                )
            }

            SettlementPartyRow(settlement = settlement)

            if (settlement.status == SettlementStatus.DISPUTED) {
                SettlementDisputeBanner(reason = settlement.disputeReason)
            }

            if (settlement.canCurrentUserConfirm || settlement.canCurrentUserDispute) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
                SettlementActionButtons(
                    canConfirm = settlement.canCurrentUserConfirm,
                    canDispute = settlement.canCurrentUserDispute,
                    onConfirm = onConfirm,
                    onDispute = onDispute
                )
            }
        }
    }
}
