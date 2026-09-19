package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel

@Suppress("LongMethod")
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
                ConsensusActionData(
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
                ConsensusActionData(
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
                ConsensusActionData(
                    label = item.nudgeButtonLabel,
                    isPrimary = false,
                    isEnabled = !item.isNudgeRateLimited && !isOffline,
                    onClick = onNudge
                )
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        val actionModifier = if (actions.size > 1) Modifier.weight(1f) else Modifier.fillMaxWidth()
        actions.forEach { action ->
            if (action.isPrimary) {
                GradientButton(
                    text = action.label,
                    onClick = action.onClick,
                    enabled = action.isEnabled,
                    modifier = actionModifier
                )
            } else {
                SecondaryButton(
                    text = action.label,
                    onClick = action.onClick,
                    enabled = action.isEnabled,
                    modifier = actionModifier
                )
            }
        }
    }
}

private data class ConsensusActionData(
    val label: String,
    val isPrimary: Boolean,
    val isEnabled: Boolean,
    val onClick: () -> Unit
)
