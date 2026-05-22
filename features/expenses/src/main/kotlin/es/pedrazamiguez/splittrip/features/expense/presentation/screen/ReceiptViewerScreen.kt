package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.transition.receiptSharedElementModifier
import es.pedrazamiguez.splittrip.features.expense.R

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 5f
private const val ZOOM_EPSILON = 0.01f

@Composable
fun ReceiptViewerScreen(
    receiptUri: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = remember { HazeState() }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                )
        ) {
            ZoomableImageContainer(
                receiptUri = receiptUri,
                hazeState = hazeState,
                onClose = onClose
            )

            ReceiptViewerTopBar(
                hazeState = hazeState,
                onClose = onClose,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun ZoomableImageContainer(
    receiptUri: String,
    hazeState: HazeState,
    onClose: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(receiptUri))
                .crossfade(true)
                .build(),
            contentDescription = stringResource(R.string.receipt_viewer_image_cd),
            modifier = Modifier
                .then(receiptSharedElementModifier(SharedElementKeys.RECEIPT_VIEWER_SHARED_ELEMENT_KEY))
                .fillMaxSize()
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
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptViewerTopBar(
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.add_expense_receipt_title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        actions = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.receipt_viewer_close_cd)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        modifier = modifier
            .horizonGlassEffect(hazeState = hazeState)
    )
}
