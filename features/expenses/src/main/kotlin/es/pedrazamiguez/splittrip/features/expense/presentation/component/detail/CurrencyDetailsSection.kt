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
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Coin
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

@Composable
internal fun CurrencyDetailsSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionIcon(TablerIcons.Outline.Coin)
            LabelText(text = stringResource(R.string.expense_detail_section_currency))
        }
        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.Default),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                if (expense.formattedSourceAmount != null) {
                    DetailRow(
                        label = "${expense.sourceCurrency} ${stringResource(R.string.expense_review_amount)}",
                        value = expense.formattedSourceAmount
                    )
                }
                if (expense.formattedExchangeRate != null) {
                    DetailRow(
                        label = stringResource(R.string.expense_review_exchange_rate),
                        value = expense.formattedExchangeRate
                    )
                }
                DetailRow(
                    label = "${expense.groupCurrency} ${stringResource(R.string.expense_review_group_amount)}",
                    value = expense.formattedGroupAmount
                )
            }
        }
    }
}
