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
import es.pedrazamiguez.splittrip.domain.enums.ExpenseAttribute
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttributesFilterSection(
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_attributes))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            ExpenseAttribute.entries.forEach { attribute ->
                val isSelected = attribute in criteria.selectedAttributes
                PassportChip(
                    label = stringResource(attribute.toStringRes()),
                    selected = isSelected,
                    onClick = {
                        onCriteriaChange(toggleExpenseAttribute(criteria, attribute))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = attribute.toIconVector(),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

private fun toggleExpenseAttribute(
    criteria: ExpenseFilterCriteria,
    attribute: ExpenseAttribute
): ExpenseFilterCriteria {
    val isSelected = attribute in criteria.selectedAttributes
    val updatedAttributes = if (isSelected) {
        criteria.selectedAttributes - attribute
    } else {
        criteria.selectedAttributes + attribute
    }
    return criteria.copy(selectedAttributes = updatedAttributes)
}
