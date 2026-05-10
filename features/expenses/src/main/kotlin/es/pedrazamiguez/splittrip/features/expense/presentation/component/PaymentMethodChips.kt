package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodChips(
    paymentMethods: List<PaymentMethodUiModel>,
    selectedPaymentMethod: PaymentMethodUiModel?,
    onPaymentMethodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        paymentMethods.forEach { method ->
            val isSelected = selectedPaymentMethod?.id == method.id
            PassportChip(
                label = method.displayText,
                selected = isSelected,
                onClick = { onPaymentMethodSelected(method.id) },
                leadingIcon = if (isSelected) {
                    { Icon(TablerIcons.Outline.Check, contentDescription = null) }
                } else {
                    null
                }
            )
        }
    }
}
