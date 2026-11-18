package es.pedrazamiguez.splittrip.features.settlement.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settlement.R

const val DISPUTE_ATTACHMENT_CONTAINER_TEST_TAG = "dispute_attachment_container"

@Composable
internal fun DisputeSettlementBottomBar(
    isSubmitEnabled: Boolean,
    isSubmitting: Boolean,
    onCancelClick: () -> Unit,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
    attachmentContent: (@Composable () -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val spacing = MaterialTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.Medium)
    ) {
        // Dedicated non-chat slot for future lightweight rich-media attachments (stickers/GIFs).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(DISPUTE_ATTACHMENT_CONTAINER_TEST_TAG)
        ) {
            attachmentContent?.invoke()
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.Small)
        ) {
            SecondaryButton(
                text = stringResource(R.string.your_balance_dispute_dialog_cancel),
                onClick = onCancelClick,
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f)
            )
            GradientButton(
                text = stringResource(R.string.your_balance_dispute_dialog_submit),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSubmitClick()
                },
                enabled = isSubmitEnabled,
                isLoading = isSubmitting,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
