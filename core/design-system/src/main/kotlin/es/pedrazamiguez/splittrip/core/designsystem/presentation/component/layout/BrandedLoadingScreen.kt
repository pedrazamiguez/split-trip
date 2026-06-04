package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout

import android.graphics.Path as AndroidPath
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/** Test tag for the branded loading screen container, used in UI tests. */
const val BRANDED_LOADING_SCREEN_TEST_TAG = "branded_loading_screen"

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

private const val MORPH_SEGMENT_DURATION_MS = 1000
private const val BREATHING_DURATION_MS = 2000
private const val BREATHING_MIN_SCALE = 0.92f
private const val BREATHING_MAX_SCALE = 1.0f

// ── Layout Constants ────────────────────────────────────────────────────

private val CONTAINER_SIZE = 180.dp
private val ICON_SIZE = 120.dp

// ── Shape Definitions ───────────────────────────────────────────────────

private fun createCircleShape() = RoundedPolygon(
    numVertices = CIRCLE_VERTEX_COUNT,
    rounding = CornerRounding(CIRCLE_CORNER_ROUNDING)
)

private fun createBlobShape() = RoundedPolygon.star(
    numVerticesPerRadius = BLOB_VERTEX_COUNT,
    radius = 1f,
    innerRadius = BLOB_INNER_RADIUS,
    rounding = CornerRounding(BLOB_CORNER_RADIUS, BLOB_CORNER_SMOOTHING)
)

private fun createFlowerShape() = RoundedPolygon.star(
    numVerticesPerRadius = FLOWER_VERTEX_COUNT,
    radius = 1f,
    innerRadius = FLOWER_INNER_RADIUS,
    rounding = CornerRounding(FLOWER_CORNER_RADIUS, FLOWER_CORNER_SMOOTHING)
)

private fun createSoftStarShape() = RoundedPolygon.star(
    numVerticesPerRadius = SOFT_STAR_VERTEX_COUNT,
    radius = 1f,
    innerRadius = SOFT_STAR_INNER_RADIUS,
    rounding = CornerRounding(SOFT_STAR_CORNER_RADIUS, SOFT_STAR_CORNER_SMOOTHING)
)

private val splashShapes = listOf(
    createCircleShape(),
    createBlobShape(),
    createFlowerShape(),
    createSoftStarShape()
)

private val splashMorphs = splashShapes.indices.map { i ->
    Morph(splashShapes[i], splashShapes[(i + 1) % splashShapes.size])
}

/**
 * A branded loading screen that displays the app icon inside a continuously
 * morphing Material 3 Expressive shape.
 *
 * The shape cycles through circle → blob → flower → soft-star and repeats
 * indefinitely, accompanied by a subtle breathing scale animation.
 *
 * **Performance:** Animation state is read exclusively inside [graphicsLayer]
 * and [drawWithContent] lambdas so that updates happen in the **draw phase**
 * only — the composable never recomposes after initial composition.
 * Pre-allocated [AndroidPath] and [Matrix] objects are reused every frame
 * to minimise GC pressure.
 *
 * This composable is **stateless** and designed to replace a plain
 * `CircularProgressIndicator` during initial app loading (e.g., while
 * resolving auth/onboarding state in `AppNavHost`).
 *
 * @param painter The [Painter] for the app icon to display inside the shape.
 * @param modifier Modifier applied to the full-screen container.
 * @param containerColor Background colour of the morphing shape.
 *   Defaults to [MaterialTheme.colorScheme.surfaceContainerHigh].
 * @param contentDescription Accessible description for screen readers.
 *   When non-null, applied to the app icon [Image]. Pass the app name or
 *   a "Loading" label so the screen is not silent for assistive technologies.
 */
@Suppress("LongMethod")
@Composable
fun BrandedLoadingScreen(
    painter: Painter,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag(BRANDED_LOADING_SCREEN_TEST_TAG),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "splash-morph")

        val morphProgressState = transition.animateFloat(
            initialValue = 0f,
            targetValue = splashMorphs.size.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = splashMorphs.size * MORPH_SEGMENT_DURATION_MS,
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
                animation = tween(
                    durationMillis = BREATHING_DURATION_MS,
                    easing = EaseInOut
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing-scale"
        )

        val reusablePath = remember { AndroidPath() }
        val composePath = remember { reusablePath.asComposePath() }
        val transformMatrix = remember { Matrix() }

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
                    val idx = progress.toInt() % splashMorphs.size
                    val t = (progress - idx.toFloat()).coerceIn(0f, 1f)

                    splashMorphs[idx].toPath(progress = t, path = reusablePath)
                    transformMatrix.reset()
                    transformMatrix.scale(size.width / 2f, size.height / 2f)
                    transformMatrix.translate(1f, 1f)
                    composePath.transform(transformMatrix)

                    clipPath(composePath) {
                        drawRect(containerColor)
                        this@drawWithContent.drawContent()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier.size(ICON_SIZE)
            )
        }
    }
}
