package es.pedrazamiguez.splittrip.features.profile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.domain.model.CropRect
import es.pedrazamiguez.splittrip.features.profile.R

private val CROP_WINDOW_SIZE = 280.dp

@Composable
internal fun AvatarCropOverlay(
    imageUri: String,
    onConfirm: (CropRect) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var intrinsicSize by remember { mutableStateOf(Size.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val layoutWidth = constraints.maxWidth.toFloat()
        val layoutHeight = constraints.maxHeight.toFloat()
        val cropSizePx = with(LocalDensity.current) { CROP_WINDOW_SIZE.toPx() }

        AvatarCropImage(
            imageUri = imageUri,
            scale = scale,
            offset = offset,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            onTransform = { s, o ->
                scale = s
                offset = o
            },
            onIntrinsicSize = { intrinsicSize = it }
        )

        AvatarCropCanvas(cropSizePx = cropSizePx)

        Text(
            text = stringResource(R.string.crop_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = MaterialTheme.spacing.Large)
        )

        AvatarCropButtons(
            onCancel = onCancel,
            onConfirm = {
                val rect = if (intrinsicSize.width > 0 && intrinsicSize.height > 0) {
                    val aspect = intrinsicSize.width / intrinsicSize.height
                    val (w, h) = if (layoutWidth / layoutHeight > aspect) {
                        layoutHeight * aspect to layoutHeight
                    } else {
                        layoutWidth to layoutWidth / aspect
                    }
                    calculateCropRect(cropSizePx, layoutWidth, layoutHeight, w, h, scale, offset)
                } else {
                    CropRect(0f, 0f, 1f, 1f)
                }
                onConfirm(rect)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

private fun calculateCropRect(
    cropSize: Float,
    layoutWidth: Float,
    layoutHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
    scale: Float,
    offset: Offset
): CropRect {
    val layoutCenterX = layoutWidth / 2f
    val layoutCenterY = layoutHeight / 2f

    val imageLeft = (layoutWidth - imageWidth) / 2f
    val imageTop = (layoutHeight - imageHeight) / 2f

    val cropLeft = (layoutWidth - cropSize) / 2f
    val cropTop = (layoutHeight - cropSize) / 2f
    val cropRight = cropLeft + cropSize
    val cropBottom = cropTop + cropSize

    fun mapX(x: Float): Float {
        val xCentered = x - layoutCenterX
        val xOffset = xCentered - offset.x
        val xScaled = xOffset / scale
        val xLayout = xScaled + layoutCenterX
        val xImg = xLayout - imageLeft
        return (xImg / imageWidth).coerceIn(0f, 1f)
    }

    fun mapY(y: Float): Float {
        val yCentered = y - layoutCenterY
        val yOffset = yCentered - offset.y
        val yScaled = yOffset / scale
        val yLayout = yScaled + layoutCenterY
        val yImg = yLayout - imageTop
        return (yImg / imageHeight).coerceIn(0f, 1f)
    }

    return CropRect(
        left = mapX(cropLeft),
        top = mapY(cropTop),
        right = mapX(cropRight),
        bottom = mapY(cropBottom)
    )
}
