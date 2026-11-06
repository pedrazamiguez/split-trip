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
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes

private val FILTERABLE_PAYMENT_STATUSES = listOf(
    PaymentStatus.FINISHED,
    PaymentStatus.SCHEDULED,
    PaymentStatus.REFUNDABLE,
    PaymentStatus.CANCELLED
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentStatusFilterSection(
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_payment_status))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            FILTERABLE_PAYMENT_STATUSES.forEach { status ->
                val isSelected = status in criteria.selectedPaymentStatuses
                PassportChip(
                    label = stringResource(status.toStringRes()),
                    selected = isSelected,
                    onClick = {
                        onCriteriaChange(togglePaymentStatus(criteria, status))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = status.toIconVector(),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private fun togglePaymentStatus(
    criteria: ExpenseFilterCriteria,
    paymentStatus: PaymentStatus
): ExpenseFilterCriteria {
    val isSelected = paymentStatus in criteria.selectedPaymentStatuses
    val updatedStatuses = if (isSelected) {
        criteria.selectedPaymentStatuses - paymentStatus
    } else {
        criteria.selectedPaymentStatuses + paymentStatus
    }
    return criteria.copy(selectedPaymentStatuses = updatedStatuses)
}
