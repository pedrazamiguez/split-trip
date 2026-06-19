package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.transition.receiptSharedElementModifier
import es.pedrazamiguez.splittrip.features.expense.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private val RECEIPT_THUMBNAIL_HEIGHT = 160.dp
private const val MIME_PDF = "application/pdf"

@Suppress("LongMethod")
@Composable
internal fun ReceiptThumbnail(
    receiptUri: String,
    mimeType: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isPdf = isPdf(receiptUri, mimeType)

    Box(
        modifier = modifier
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
            val context = LocalContext.current
            var pdfBitmap by remember(receiptUri) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(receiptUri) {
                val rendered = withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(receiptUri)
                        renderPdfFirstPage(context, uri)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to render PDF thumbnail for $receiptUri")
                        null
                    }
                }
                pdfBitmap = rendered
            }

            if (pdfBitmap != null) {
                Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.expense_detail_receipt_thumbnail_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(MaterialTheme.shapes.large)
                )
            } else {
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
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(receiptUri))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.expense_detail_receipt_thumbnail_cd),
                contentScale = ContentScale.Crop,
                modifier = receiptSharedElementModifier(SharedElementKeys.RECEIPT_VIEWER_SHARED_ELEMENT_KEY)
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.large)
            )
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

private fun renderPdfFirstPage(context: Context, uri: Uri): Bitmap? {
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
                val canvas = Canvas(destBitmap)
                canvas.drawColor(Color.WHITE)
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
