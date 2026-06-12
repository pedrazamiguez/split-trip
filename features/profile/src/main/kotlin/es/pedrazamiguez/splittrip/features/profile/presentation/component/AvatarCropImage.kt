package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
                    val maxPanX = layoutWidth * newScale / 2f
                    val maxPanY = layoutHeight * newScale / 2f
                    val newOffset = Offset(
                        x = (offset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (offset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                    onTransform(newScale, newOffset)
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
