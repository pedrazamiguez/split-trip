package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.badge.ProBadge
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R

/**
 * Receipt image picker with a section title.
 */
@Composable
internal fun ReceiptSection(
    receiptUri: String?,
    mimeType: String?,
    onPickerRequested: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
    onViewImage: (() -> Unit)? = null,
    remainingAttachmentsLabel: String? = null,
    isQuotaExhausted: Boolean = false,
    isProFeature: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeadingText(text = stringResource(R.string.add_expense_receipt_title))
            if (isProFeature) {
                ProBadge()
            }
        }
        Text(
            text = stringResource(R.string.add_expense_receipt_manual_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (remainingAttachmentsLabel != null) {
            ManualReceiptQuotaBadge(
                label = remainingAttachmentsLabel,
                isExhausted = isQuotaExhausted
            )
        }
        ReceiptImagePicker(
            receiptUri = receiptUri,
            mimeType = mimeType,
            onPickerRequested = onPickerRequested,
            onRemoveImage = onRemoveImage,
            onViewImage = onViewImage,
            isLocked = isProFeature
        )
    }
}
