package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SyncStatusIndicator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

@Composable
internal fun ProvenanceSection(expense: ExpenseDetailUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.ExtraSmall,
                vertical = MaterialTheme.spacing.Small
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
            CaptionText(
                text = stringResource(R.string.expense_detail_created_by, expense.createdByText),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CaptionText(
                text = expense.createdAtText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SyncStatusIndicator(syncStatus = expense.syncStatus)
    }
}
