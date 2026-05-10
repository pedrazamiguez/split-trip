package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.expense.R

/**
 * Receipt image picker with a section title.
 */
@Composable
internal fun ReceiptSection(
    receiptUri: String?,
    onImageSelected: (String) -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.add_expense_receipt_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        ReceiptImagePicker(
            receiptUri = receiptUri,
            onImageSelected = onImageSelected,
            onRemoveImage = onRemoveImage
        )
    }
}
