package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.expense.R
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 5f
private const val ZOOM_EPSILON = 0.01f
private const val HTTP_TIMEOUT_MS = 10000

@Composable
internal fun PdfViewerContent(
    pdfUriString: String,
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUriString) {
        isLoading = true
        hasError = false
        withContext(Dispatchers.IO) {
            try {
                pages = loadPdfPages(context, pdfUriString)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load/render PDF in viewer")
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    when {
        isLoading -> PdfLoadingView(hazeState = hazeState, modifier = modifier)
        hasError || pages.isEmpty() -> PdfErrorView(hazeState = hazeState, modifier = modifier)
        else -> PdfPagesListView(
            pages = pages,
            hazeState = hazeState,
            onClose = onClose,
            modifier = modifier
        )
    }
}

@Composable
private fun PdfLoadingView(hazeState: HazeState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(state = hazeState),
        contentAlignment = Alignment.Center
    ) {
        ShimmerLoadingList()
    }
}

@Composable
private fun PdfErrorView(hazeState: HazeState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(state = hazeState),
        contentAlignment = Alignment.Center
    ) {
        EmptyStateView(
            title = stringResource(R.string.receipt_viewer_pdf_error),
            icon = TablerIcons.Outline.Receipt
        )
    }
}

@Composable
private fun PdfPagesListView(
    pages: List<Bitmap>,
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val targetScale = (scale * zoom).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                    if (targetScale <= MIN_ZOOM_SCALE + ZOOM_EPSILON) {
                        scale = MIN_ZOOM_SCALE
                        offset = Offset.Zero
                    } else {
                        scale = targetScale
                        offset += pan
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (scale <= MIN_ZOOM_SCALE + ZOOM_EPSILON) {
                        onClose()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 80.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = scale <= MIN_ZOOM_SCALE + ZOOM_EPSILON
        ) {
            items(pages.size) { index ->
                PdfPageItem(bitmap = pages[index], pageNumber = index + 1)
            }
        }
    }
}

@Composable
private fun PdfPageItem(bitmap: Bitmap, pageNumber: Int, modifier: Modifier = Modifier) {
    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        ghostBorder = true
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.receipt_viewer_pdf_page_cd, pageNumber),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth
        )
    }
}

private suspend fun loadPdfPages(context: Context, pdfUriString: String): List<Bitmap> {
    val uri = Uri.parse(pdfUriString)
    val targetUri = if (uri.scheme == "http" || uri.scheme == "https") {
        val tempFile = File(context.cacheDir, "temp_viewer_${UUID.randomUUID()}.pdf")
        downloadUrlToFile(pdfUriString, tempFile)
        Uri.fromFile(tempFile)
    } else {
        uri
    }

    val pfd = context.contentResolver.openFileDescriptor(targetUri, "r") ?: return emptyList()
    var renderer: PdfRenderer? = null
    try {
        renderer = PdfRenderer(pfd)
        val list = mutableListOf<Bitmap>()
        for (i in 0 until renderer.pageCount) {
            list.add(renderPageToBitmap(renderer, i))
        }
        return list
    } finally {
        closeRendererAndPfd(renderer, pfd)
    }
}

private fun renderPageToBitmap(renderer: PdfRenderer, pageIndex: Int): Bitmap {
    var page: PdfRenderer.Page? = null
    try {
        page = renderer.openPage(pageIndex)
        val width = page.width * 2
        val height = page.height * 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    } finally {
        page?.close()
    }
}

private fun downloadUrlToFile(remoteUrl: String, tempFile: File) {
    val url = java.net.URL(remoteUrl)
    val connection = url.openConnection() as java.net.HttpURLConnection
    try {
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
            error("HTTP error code: ${connection.responseCode}")
        }
        connection.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
    } finally {
        connection.disconnect()
    }
}

private fun closeRendererAndPfd(renderer: PdfRenderer?, pfd: ParcelFileDescriptor) {
    try {
        renderer?.close()
    } catch (e: Exception) {
        Timber.e(e, "Failed to close PdfRenderer")
    }
    try {
        pfd.close()
    } catch (e: Exception) {
        Timber.e(e, "Failed to close ParcelFileDescriptor")
    }
}
