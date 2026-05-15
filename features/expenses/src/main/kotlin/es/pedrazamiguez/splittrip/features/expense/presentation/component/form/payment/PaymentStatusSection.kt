package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.chips.CondensedChips
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
        SectionHeadingText(text = stringResource(R.string.add_expense_payment_status_title))

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
