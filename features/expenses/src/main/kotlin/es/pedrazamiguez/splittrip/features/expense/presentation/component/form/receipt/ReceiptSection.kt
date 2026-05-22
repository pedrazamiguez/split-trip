package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
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
    onViewImage: (() -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SectionHeadingText(text = stringResource(R.string.add_expense_receipt_title))
        ReceiptImagePicker(
            receiptUri = receiptUri,
            mimeType = mimeType,
            onPickerRequested = onPickerRequested,
            onRemoveImage = onRemoveImage,
            onViewImage = onViewImage
        )
    }
}
