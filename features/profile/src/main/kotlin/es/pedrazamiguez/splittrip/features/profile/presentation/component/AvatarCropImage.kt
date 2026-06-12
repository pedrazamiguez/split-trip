package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f

@Composable
internal fun AvatarCropImage(
    imageUri: String,
    scale: Float,
    offset: Offset,
    layoutWidth: Float,
    layoutHeight: Float,
    onTransform: (Float, Offset) -> Unit,
    onIntrinsicSize: (Size) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)
    val currentLayoutWidth by rememberUpdatedState(layoutWidth)
    val currentLayoutHeight by rememberUpdatedState(layoutHeight)
    val currentOnTransform by rememberUpdatedState(onTransform)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val newScale = (currentScale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                    val maxPanX = currentLayoutWidth * newScale / 2f
                    val maxPanY = currentLayoutHeight * newScale / 2f
                    val newOffset = Offset(
                        x = (currentOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (currentOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                    currentOnTransform(newScale, newOffset)
                }
            }
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            onState = { state ->
                if (state is AsyncImagePainter.State.Success) {
                    onIntrinsicSize(state.painter.intrinsicSize)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
