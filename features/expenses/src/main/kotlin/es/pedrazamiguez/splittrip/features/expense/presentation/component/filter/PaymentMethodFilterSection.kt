package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentMethodFilterSection(
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_payment_method))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            PaymentMethod.entries.forEach { method ->
                val isSelected = method in criteria.selectedPaymentMethods
                PassportChip(
                    label = stringResource(method.toStringRes()),
                    selected = isSelected,
                    onClick = {
                        onCriteriaChange(togglePaymentMethod(criteria, method))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = method.toIconVector(),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private fun togglePaymentMethod(
    criteria: ExpenseFilterCriteria,
    paymentMethod: PaymentMethod
): ExpenseFilterCriteria {
    val isSelected = paymentMethod in criteria.selectedPaymentMethods
    val updatedMethods = if (isSelected) {
        criteria.selectedPaymentMethods - paymentMethod
    } else {
        criteria.selectedPaymentMethods + paymentMethod
    }
    return criteria.copy(selectedPaymentMethods = updatedMethods)
}
