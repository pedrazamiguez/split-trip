package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementCardAction
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel

@Composable
internal fun ConsensusCardActions(
    item: SettlementConsensusItemUiModel,
    onConfirm: () -> Unit,
    onDispute: () -> Unit,
    onNudge: () -> Unit,
    modifier: Modifier = Modifier,
    isOffline: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val disputeLabel = stringResource(R.string.your_balance_settlement_dispute)
    val actions = buildList {
        if (item.canConfirm) {
            add(
                SettlementCardAction(
                    label = item.confirmLabel,
                    isPrimary = true,
                    isEnabled = !isOffline,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onConfirm()
                    }
                )
            )
        }
        if (item.canDispute) {
            add(
                SettlementCardAction(
                    label = disputeLabel,
                    isPrimary = false,
                    isEnabled = !isOffline,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDispute()
                    }
                )
            )
        }
        if (item.canNudge) {
            add(
                SettlementCardAction(
                    label = item.nudgeButtonLabel,
                    isPrimary = false,
                    isEnabled = !item.isNudgeRateLimited && !isOffline,
                    onClick = onNudge
                )
            )
        }
    }

    SettlementCardActionRow(
        actions = actions,
        modifier = modifier
    )
}
