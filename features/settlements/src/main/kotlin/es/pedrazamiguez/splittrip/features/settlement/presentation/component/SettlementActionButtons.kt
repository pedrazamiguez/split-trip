package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementCardAction

@Composable
internal fun SettlementActionButtons(
    canConfirm: Boolean,
    canDispute: Boolean,
    onConfirm: () -> Unit,
    onDispute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val confirmLabel = stringResource(R.string.settlement_overview_confirm)
    val disputeLabel = stringResource(R.string.settlement_overview_dispute)
    val actions = buildList {
        if (canConfirm) {
            add(
                SettlementCardAction(
                    label = confirmLabel,
                    isPrimary = true,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onConfirm()
                    }
                )
            )
        }
        if (canDispute) {
            add(
                SettlementCardAction(
                    label = disputeLabel,
                    isPrimary = false,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDispute()
                    }
                )
            )
        }
    }

    SettlementCardActionRow(
        actions = actions,
        modifier = modifier
    )
}
