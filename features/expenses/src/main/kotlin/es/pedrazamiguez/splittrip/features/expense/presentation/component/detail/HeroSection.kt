package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.transition.receiptSharedElementModifier
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private val HERO_AMOUNT_SIZE = 40.sp
private val RECEIPT_THUMBNAIL_HEIGHT = 160.dp

@Composable
internal fun HeroSection(
    expense: ExpenseDetailUiModel,
    onReceiptTap: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
        HeroTagRow(expense)
        // §4.4 ambient shadow: hero is the only screen element warranting an inset
        // tier so the page's information hierarchy reads top-to-bottom by elevation.
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

private fun isPdf(uriString: String, mimeType: String?): Boolean {
    if (mimeType == MIME_PDF) return true
    val uri = Uri.parse(uriString)
    val path = if (uri.scheme == "file") uri.path.orEmpty() else uriString
    if (path.lowercase().endsWith(".pdf")) return true
    val lastSegment = uri.lastPathSegment.orEmpty().lowercase()
    if (lastSegment.endsWith(".pdf") || lastSegment.contains(".pdf?")) return true
    return false
}

private fun renderPdfFirstPage(context: android.content.Context, uri: Uri): Bitmap? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    var page: PdfRenderer.Page? = null
    try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd != null) {
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                page = renderer.openPage(0)
                val aspectRatio = page.width.toFloat() / page.height.toFloat()
                val targetWidth = (aspectRatio * 480).toInt()
                val destBitmap = Bitmap.createBitmap(
                    targetWidth.coerceAtLeast(1),
                    480,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(destBitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(destBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return destBitmap
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to render PDF page for $uri")
    } finally {
        try {
            page?.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
        try {
            renderer?.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
        try {
            pfd?.close()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
    return null
}

@Composable
private fun rememberPdfThumbnail(pdfUriString: String): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(pdfUriString) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(pdfUriString) {
        val rendered = withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(pdfUriString)
                renderPdfFirstPage(context, uri)
            } catch (e: Exception) {
                Timber.e(e, "Failed to render PDF thumbnail for $pdfUriString")
                null
            }
        }
        bitmap = rendered
    }
    return bitmap
}

@Composable
private fun ReceiptThumbnail(
    receiptUri: String,
    mimeType: String?,
    onClick: (() -> Unit)? = null
) {
    val isPdf = isPdf(receiptUri, mimeType)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(RECEIPT_THUMBNAIL_HEIGHT)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        if (isPdf) {
            PdfThumbnailContent(receiptUri = receiptUri)
        } else {
            ImageThumbnailContent(
                receiptUri = receiptUri,
                modifier = receiptSharedElementModifier(SharedElementKeys.RECEIPT_VIEWER_SHARED_ELEMENT_KEY)
            )
        }
    }
}

@Composable
private fun BoxScope.PdfThumbnailContent(receiptUri: String) {
    val pdfBitmap = rememberPdfThumbnail(receiptUri)
    if (pdfBitmap != null) {
        Image(
            bitmap = pdfBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.expense_detail_receipt_thumbnail_cd),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.large)
        )
    } else {
        // PDFs cannot be rendered inline — show an informational placeholder instead.
        Column(
            modifier = Modifier.matchParentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Receipt,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BodyText(
                text = stringResource(R.string.expense_detail_receipt_pdf_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BoxScope.ImageThumbnailContent(
    receiptUri: String,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(Uri.parse(receiptUri))
            .crossfade(true)
            .build(),
        contentDescription = stringResource(R.string.expense_detail_receipt_thumbnail_cd),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .matchParentSize()
            .clip(MaterialTheme.shapes.large)
    )
}

private const val MIME_PDF = "application/pdf"
