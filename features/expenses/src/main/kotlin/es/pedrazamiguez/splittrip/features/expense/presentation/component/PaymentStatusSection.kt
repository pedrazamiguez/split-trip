package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Payment status selection using condensed chips.
 */
@Composable
internal fun PaymentStatusSection(
    availablePaymentStatuses: ImmutableList<PaymentStatusUiModel>,
    selectedPaymentStatus: PaymentStatusUiModel?,
    onPaymentStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.add_expense_payment_status_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        CondensedChips(
            items = availablePaymentStatuses,
            selectedId = selectedPaymentStatus?.id,
            onItemSelected = { statusId ->
                onPaymentStatusSelected(statusId)
                focusManager.clearFocus()
            },
            itemId = { it.id },
            itemLabel = { it.displayText },
            visibleCount = 3
        )
    }
}
