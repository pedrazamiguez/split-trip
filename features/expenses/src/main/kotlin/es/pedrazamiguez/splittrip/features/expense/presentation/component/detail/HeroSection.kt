package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

private val HERO_AMOUNT_SIZE = 40.sp
private val RECEIPT_THUMBNAIL_HEIGHT = 160.dp

@Composable
internal fun HeroSection(expense: ExpenseDetailUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
        HeroTagRow(expense)
        // §4.4 ambient shadow: hero is the only screen element warranting an inset
        // tier so the page's information hierarchy reads top-to-bottom by elevation.
        FlatCard(modifier = Modifier.fillMaxWidth(), elevation = 8.dp) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.Default)) {
                if (expense.receiptUri != null) {
                    ReceiptThumbnail(expense.receiptUri)
                    Spacer(Modifier.height(MaterialTheme.spacing.Medium))
                }
                HeroAmountContent(expense)
            }
        }
    }
}

@Composable
private fun HeroTagRow(expense: ExpenseDetailUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left column: identity chips (category + payment method)
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

        // Right column: state chips (payment status + date)
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
}

@Composable
private fun HeroAmountContent(expense: ExpenseDetailUiModel) {
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
        textAlign = TextAlign.Center
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
        ForeignCurrencyRow(expense)
    }
}

@Composable
private fun ForeignCurrencyRow(expense: ExpenseDetailUiModel) {
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
            // Tabular numerals via AmountText keep decimal separators aligned with
            // amounts elsewhere on the screen (Horizon Narrative §3.4).
            AmountText(
                text = expense.formattedSourceAmount!!,
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

@Composable
private fun ReceiptThumbnail(receiptUri: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RECEIPT_THUMBNAIL_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(receiptUri))
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.expense_detail_receipt_thumbnail_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.large)
        )
    }
}
