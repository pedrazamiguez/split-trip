package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.horizonGlassEffect
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.expense.R

private const val MIN_ZOOM_SCALE = 1f
private const val MAX_ZOOM_SCALE = 5f

@Composable
fun ReceiptViewerScreen(
    receiptUri: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hazeState = remember { HazeState() }

    SharedTransitionSurface(
        sharedElementKey = SharedElementKeys.RECEIPT_VIEWER_SHARED_ELEMENT_KEY,
        color = Color.Black,
        modifier = modifier
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
                hazeState = hazeState
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
    hazeState: HazeState
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
            model = receiptUri,
            contentDescription = stringResource(R.string.receipt_viewer_image_cd),
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                        if (scale > MIN_ZOOM_SCALE) {
                            offset += pan
                        } else {
                            offset = Offset.Zero
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
                    onClick = {} // No-op to block background tap from closing
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ReceiptViewerTopBar(
    hazeState: HazeState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .horizonGlassEffect(hazeState = hazeState)
            .statusBarsPadding()
            .padding(MaterialTheme.spacing.Medium)
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = TablerIcons.Outline.X,
                contentDescription = stringResource(R.string.receipt_viewer_close_cd),
                tint = Color.White
            )
        }
    }
}
