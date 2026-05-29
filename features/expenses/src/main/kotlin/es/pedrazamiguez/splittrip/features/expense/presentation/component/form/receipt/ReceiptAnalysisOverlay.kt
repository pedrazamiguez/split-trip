package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt

import android.graphics.Path as AndroidPath
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.features.expense.R

/** Test tag for the receipt analysis overlay container, used in UI tests. */
const val RECEIPT_ANALYSIS_OVERLAY_TEST_TAG = "receipt_analysis_overlay"

// ── Shape Constants ─────────────────────────────────────────────────────

private const val CIRCLE_VERTEX_COUNT = 8
private const val CIRCLE_CORNER_ROUNDING = 0.8f

private const val BLOB_VERTEX_COUNT = 7
private const val BLOB_INNER_RADIUS = 0.9f
private const val BLOB_CORNER_RADIUS = 0.5f
private const val BLOB_CORNER_SMOOTHING = 0.5f

private const val FLOWER_VERTEX_COUNT = 5
private const val FLOWER_INNER_RADIUS = 0.75f
private const val FLOWER_CORNER_RADIUS = 0.4f
private const val FLOWER_CORNER_SMOOTHING = 0.4f

private const val SOFT_STAR_VERTEX_COUNT = 6
private const val SOFT_STAR_INNER_RADIUS = 0.85f
private const val SOFT_STAR_CORNER_RADIUS = 0.3f
private const val SOFT_STAR_CORNER_SMOOTHING = 0.3f

// ── Animation Constants ─────────────────────────────────────────────────

private const val MORPH_SEGMENT_DURATION_MS = 900
private const val BREATHING_DURATION_MS = 2200
private const val BREATHING_MIN_SCALE = 0.94f
private const val BREATHING_MAX_SCALE = 1.0f
private const val SCAN_DURATION_MS = 1400

private const val SCAN_GLOW_HEIGHT_FRACTION = 0.20f
private const val SCAN_CORE_HEIGHT_FRACTION = 0.03f
private const val SCAN_GLOW_ALPHA = 0.22f
private const val SCAN_CORE_ALPHA = 0.9f
private const val SCRIM_ALPHA = 0.86f

// ── Layout Constants ────────────────────────────────────────────────────

private val CONTAINER_SIZE = 200.dp
private val ICON_SIZE = 88.dp

// ── Shape Definitions ───────────────────────────────────────────────────

private val analysisShapes = listOf(
    RoundedPolygon(
        numVertices = CIRCLE_VERTEX_COUNT,
        rounding = CornerRounding(CIRCLE_CORNER_ROUNDING)
    ),
    RoundedPolygon.star(
        numVerticesPerRadius = BLOB_VERTEX_COUNT,
        radius = 1f,
        innerRadius = BLOB_INNER_RADIUS,
        rounding = CornerRounding(BLOB_CORNER_RADIUS, BLOB_CORNER_SMOOTHING)
    ),
    RoundedPolygon.star(
        numVerticesPerRadius = FLOWER_VERTEX_COUNT,
        radius = 1f,
        innerRadius = FLOWER_INNER_RADIUS,
        rounding = CornerRounding(FLOWER_CORNER_RADIUS, FLOWER_CORNER_SMOOTHING)
    ),
    RoundedPolygon.star(
        numVerticesPerRadius = SOFT_STAR_VERTEX_COUNT,
        radius = 1f,
        innerRadius = SOFT_STAR_INNER_RADIUS,
        rounding = CornerRounding(SOFT_STAR_CORNER_RADIUS, SOFT_STAR_CORNER_SMOOTHING)
    )
)

private val analysisMorphs = analysisShapes.indices.map { i ->
    Morph(analysisShapes[i], analysisShapes[(i + 1) % analysisShapes.size])
}

/**
 * Full-screen blocking overlay shown while on-device AI extracts receipt fields.
 * Blocks back navigation and all pointer input until [visible] turns false.
 */
@Composable
fun ReceiptAnalysisOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        BackHandler(enabled = visible) {}

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                        }
                    }
                }
                .testTag(RECEIPT_ANALYSIS_OVERLAY_TEST_TAG),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ScanningMorphShape()
                BodyText(
                    text = stringResource(R.string.expense_autofill_in_progress),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.ExtraLarge)
                )
            }
        }
    }
}

@Composable
private fun ScanningMorphShape() {
    val transition = rememberInfiniteTransition(label = "receipt-analysis")

    val morphProgressState = transition.animateFloat(
        initialValue = 0f,
        targetValue = analysisMorphs.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = analysisMorphs.size * MORPH_SEGMENT_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "morph-progress"
    )

    val scaleState = transition.animateFloat(
        initialValue = BREATHING_MIN_SCALE,
        targetValue = BREATHING_MAX_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATHING_DURATION_MS, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing-scale"
    )

    val scanState = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SCAN_DURATION_MS, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan-sweep"
    )

    MorphScanBox(
        morphProgressState = morphProgressState,
        scaleState = scaleState,
        scanState = scanState
    )
}

// Animation State is read only in the draw phase; paths/matrix are reused to avoid per-frame GC.
@Composable
private fun MorphScanBox(
    morphProgressState: State<Float>,
    scaleState: State<Float>,
    scanState: State<Float>
) {
    val reusablePath = remember { AndroidPath() }
    val composePath = remember { reusablePath.asComposePath() }
    val transformMatrix = remember { Matrix() }

    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val gradientBrush = remember(gradientColors) { Brush.linearGradient(gradientColors) }
    val glowColor = Color.White.copy(alpha = SCAN_GLOW_ALPHA)
    val coreColor = Color.White.copy(alpha = SCAN_CORE_ALPHA)

    Box(
        modifier = Modifier
            .size(CONTAINER_SIZE)
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            }
            .drawWithContent {
                val progress = morphProgressState.value
                val idx = progress.toInt() % analysisMorphs.size
                val segment = (progress - idx.toFloat()).coerceIn(0f, 1f)

                analysisMorphs[idx].toPath(progress = segment, path = reusablePath)
                transformMatrix.reset()
                transformMatrix.scale(size.width / 2f, size.height / 2f)
                transformMatrix.translate(1f, 1f)
                composePath.transform(transformMatrix)

                clipPath(composePath) {
                    drawRect(brush = gradientBrush)

                    val centerY = scanState.value * size.height
                    val glowHeight = size.height * SCAN_GLOW_HEIGHT_FRACTION
                    val coreHeight = size.height * SCAN_CORE_HEIGHT_FRACTION
                    drawRect(
                        color = glowColor,
                        topLeft = Offset(0f, centerY - glowHeight / 2f),
                        size = Size(size.width, glowHeight)
                    )
                    drawRect(
                        color = coreColor,
                        topLeft = Offset(0f, centerY - coreHeight / 2f),
                        size = Size(size.width, coreHeight)
                    )

                    this@drawWithContent.drawContent()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = TablerIcons.Outline.PhotoAi,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(ICON_SIZE)
        )
    }
}
