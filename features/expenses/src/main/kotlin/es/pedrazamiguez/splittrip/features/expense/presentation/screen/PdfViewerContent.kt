package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.expense.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 5f
private const val ZOOM_EPSILON = 0.01f
private const val HTTP_TIMEOUT_MS = 10000
private const val A4_ASPECT_RATIO = 0.707f
private val FLOATING_TOP_BAR_HEIGHT = 64.dp

private class PdfResourceHolder(
    val renderer: PdfRenderer,
    val pfd: ParcelFileDescriptor,
    val tempFile: File?
) {
    fun close() {
        try {
            renderer.close()
        } catch (e: Exception) {
            Timber.e(e, "Failed to close PdfRenderer")
        }
        try {
            pfd.close()
        } catch (e: Exception) {
            Timber.e(e, "Failed to close ParcelFileDescriptor")
        }
        tempFile?.delete()
    }
}

@Composable
internal fun PdfViewerContent(
    pdfUriString: String,
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var resourceHolder by remember { mutableStateOf<PdfResourceHolder?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUriString) {
        isLoading = true
        hasError = false
        withContext(Dispatchers.IO) {
            try {
                resourceHolder = initializePdfResources(context, pdfUriString)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize PDF resource holder")
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(pdfUriString) {
        onDispose {
            resourceHolder?.close()
            resourceHolder = null
        }
    }

    val currentResourceHolder = resourceHolder

    when {
        isLoading -> PdfLoadingView(hazeState = hazeState, modifier = modifier)
        hasError || currentResourceHolder == null -> PdfErrorView(hazeState = hazeState, modifier = modifier)
        else -> PdfPagesListView(
            resourceHolder = currentResourceHolder,
            hazeState = hazeState,
            onClose = onClose,
            modifier = modifier
        )
    }
}

private fun initializePdfResources(context: Context, pdfUriString: String): PdfResourceHolder {
    var localTempFile: File? = null
    var localPfd: ParcelFileDescriptor? = null
    var localRenderer: PdfRenderer? = null
    try {
        val uri = Uri.parse(pdfUriString)
        val isRemote = uri.scheme == "http" || uri.scheme == "https"
        if (isRemote) {
            localTempFile = File(context.cacheDir, "temp_viewer_${UUID.randomUUID()}.pdf")
            downloadUrlToFile(pdfUriString, localTempFile)
        }
        val targetUri = if (localTempFile != null) Uri.fromFile(localTempFile) else uri
        localPfd = checkNotNull(context.contentResolver.openFileDescriptor(targetUri, "r")) {
            "Could not open file descriptor"
        }
        localRenderer = PdfRenderer(localPfd)
        return PdfResourceHolder(localRenderer, localPfd, localTempFile)
    } catch (e: Exception) {
        localRenderer?.close()
        localPfd?.close()
        localTempFile?.delete()
        throw e
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
    resourceHolder: PdfResourceHolder,
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val bottomPadding = LocalBottomPadding.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = FLOATING_TOP_BAR_HEIGHT + statusBarHeight + MaterialTheme.spacing.Default
    val finalBottomPadding = bottomPadding + MaterialTheme.spacing.Default
    val mutex = remember { Mutex() }

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
        PdfLazyColumn(
            resourceHolder = resourceHolder,
            topPadding = topPadding,
            bottomPadding = finalBottomPadding,
            scrollEnabled = scale <= MIN_ZOOM_SCALE + ZOOM_EPSILON,
            mutex = mutex
        )
    }
}

@Composable
private fun PdfLazyColumn(
    resourceHolder: PdfResourceHolder,
    topPadding: Dp,
    bottomPadding: Dp,
    scrollEnabled: Boolean,
    mutex: Mutex,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = topPadding,
            bottom = bottomPadding,
            start = MaterialTheme.spacing.Default,
            end = MaterialTheme.spacing.Default
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
        userScrollEnabled = scrollEnabled
    ) {
        items(resourceHolder.renderer.pageCount) { index ->
            PdfPageItem(
                renderer = resourceHolder.renderer,
                mutex = mutex,
                pageIndex = index
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    renderer: PdfRenderer,
    mutex: Mutex,
    pageIndex: Int,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                mutex.withLock {
                    bitmap = renderPageToBitmap(renderer, pageIndex)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to render PDF page $pageIndex")
                hasError = true
            }
        }
    }

    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        ghostBorder = true
    ) {
        when {
            hasError -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(A4_ASPECT_RATIO),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateView(
                        title = stringResource(R.string.receipt_viewer_pdf_error),
                        icon = TablerIcons.Outline.Receipt
                    )
                }
            }
            bitmap == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(A4_ASPECT_RATIO),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerLoadingList()
                }
            }
            else -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = stringResource(R.string.receipt_viewer_pdf_page_cd, pageIndex + 1),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }
}

private fun renderPageToBitmap(renderer: PdfRenderer, pageIndex: Int): Bitmap {
    var page: PdfRenderer.Page? = null
    try {
        page = renderer.openPage(pageIndex)
        val maxDimension = 2048
        var width = page.width * 2
        var height = page.height * 2
        val maxDim = maxOf(width, height)
        if (maxDim > maxDimension) {
            val scale = maxDimension.toDouble() / maxDim
            width = (width * scale).toInt().coerceAtLeast(1)
            height = (height * scale).toInt().coerceAtLeast(1)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    } finally {
        page?.close()
    }
}

private fun downloadUrlToFile(remoteUrl: String, tempFile: File) {
    val url = URL(remoteUrl)
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = HTTP_TIMEOUT_MS
        connection.readTimeout = HTTP_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
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
