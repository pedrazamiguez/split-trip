package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

@Composable
internal fun HeroSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            CategoryChip(
                icon = expense.category.toIconVector(),
                label = expense.categoryText
            )
            StatusChip(text = expense.paymentStatusText)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Calendar,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CaptionText(
                    text = expense.dateText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FlatCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.Default)) {
                CaptionText(
                    text = stringResource(R.string.expense_review_amount).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = expense.formattedGroupAmount,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 40.sp
                )
                CaptionText(
                    text = expense.paidByText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (expense.isForeignCurrency && expense.formattedSourceAmount != null) {
                    Spacer(Modifier.height(MaterialTheme.spacing.Small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CaptionText(
                            text = stringResource(R.string.expense_detail_group_currency_label),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            BodyText(
                                text = expense.formattedSourceAmount,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (expense.formattedExchangeRate != null) {
                                CaptionText(
                                    text = stringResource(
                                        R.string.expense_detail_rate_label,
                                        expense.formattedExchangeRate
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
