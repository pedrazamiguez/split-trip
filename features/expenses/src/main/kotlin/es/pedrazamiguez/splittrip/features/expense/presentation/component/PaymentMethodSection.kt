package es.pedrazamiguez.splittrip.features.expense.presentation.component

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
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Payment method selection using condensed chips.
 */
@Composable
internal fun PaymentMethodSection(
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    selectedPaymentMethod: PaymentMethodUiModel?,
    onPaymentMethodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        SectionHeadingText(text = stringResource(R.string.add_expense_payment_method_title))

        CondensedChips(
            items = paymentMethods,
            selectedId = selectedPaymentMethod?.id,
            onItemSelected = { methodId ->
                onPaymentMethodSelected(methodId)
                focusManager.clearFocus()
            },
            itemId = { it.id },
            itemLabel = { it.displayText },
            visibleCount = 6
        )
    }
}
