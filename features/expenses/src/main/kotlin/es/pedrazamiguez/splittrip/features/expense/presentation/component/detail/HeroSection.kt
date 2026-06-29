package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

private val HERO_AMOUNT_SIZE = 40.sp

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
internal fun HeroSection(
    expense: ExpenseDetailUiModel,
    onReceiptTap: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
        // Tag Row (previously HeroTagRow)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                CategoryChip(
                    icon = expense.category.toIconVector(),
                    label = expense.categoryText
                )
                MethodChip(
                    icon = expense.paymentMethodIcon,
                    label = expense.paymentMethodText
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                StatusBadgeChip(
                    icon = expense.paymentStatusIcon,
                    label = expense.paymentStatusText
                )
                DateChip(text = expense.dateText)
            }
        }

        // Amount Content inside FlatCard (previously HeroAmountContent)
        FlatCard(modifier = Modifier.fillMaxWidth(), elevation = 8.dp) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.Default)) {
                if (expense.receiptUri != null) {
                    ReceiptThumbnail(
                        receiptUri = expense.receiptUri,
                        mimeType = expense.receiptMimeType,
                        onClick = onReceiptTap
                    )
                    Spacer(Modifier.height(MaterialTheme.spacing.Medium))
                }

                Text(
                    text = expense.expenseScopeLabel.uppercase(),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = expense.formattedGroupAmount,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = HERO_AMOUNT_SIZE,
                    textAlign = TextAlign.Center,
                    textDecoration = if (expense.isCancelled) TextDecoration.LineThrough else null
                )
                Text(
                    text = expense.paidByText,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (expense.vendorText != null) {
                    Text(
                        text = expense.vendorText,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (expense.isForeignCurrency && expense.formattedSourceAmount != null) {
                    Spacer(Modifier.height(MaterialTheme.spacing.Small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CaptionText(
                            text = stringResource(
                                R.string.expense_detail_amount_in_currency,
                                expense.sourceCurrency
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            AmountText(
                                text = expense.formattedSourceAmount,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = if (expense.isCancelled) TextDecoration.LineThrough else null
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
