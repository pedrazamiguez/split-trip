package es.pedrazamiguez.splittrip.core.designsystem.presentation.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val PILL_AUTO_DISMISS_MS = 3000L
private val PILL_SHAPE = RoundedCornerShape(28.dp)

/**
 * State for a single pill notification.
 */
data class PillState(
    val message: String,
    val id: Long = System.nanoTime()
)

/**
 * Controller for showing top-pill notifications that survive navigation between screens.
 *
 * Pills drop from the top of the screen instead of popping from the bottom,
 * avoiding overlap with navigation bars and action buttons.
 */
@Stable
class TopPillController(private val scope: CoroutineScope) {

    private val _pillState = MutableStateFlow<PillState?>(null)
    val pillState: StateFlow<PillState?> = _pillState.asStateFlow()

    /**
     * Shows a pill notification that auto-dismisses after [PILL_AUTO_DISMISS_MS].
     * This method returns immediately and doesn't suspend.
     */
    fun showPill(message: String) {
        scope.launch {
            _pillState.value = PillState(message = message)
        }
    }

    fun dismiss() {
        _pillState.value = null
    }
}

/**
 * Creates and remembers a [TopPillController] with its associated scope.
 * Should be called at the MainScreen level.
 */
@Composable
fun rememberTopPillController(): TopPillController {
    val scope = rememberCoroutineScope()
    return remember(scope) { TopPillController(scope) }
}

/**
 * CompositionLocal for providing a [TopPillController] to child composables.
 *
 * Usage in Feature:
 * ```
 * val pillController = LocalTopPillController.current
 * pillController.showPill(message = action.message.asString(context))
 * ```
 */
val LocalTopPillController = compositionLocalOf<TopPillController> {
    error(
        "No TopPillController provided. Make sure to wrap your content " +
            "with a provider that sets LocalTopPillController."
    )
}

/**
 * An animated top pill notification that drops from the top of the screen.
 *
 * Place this at the top of your content `Box` (above other content) so it overlays.
 * Auto-dismisses after [PILL_AUTO_DISMISS_MS].
 */
@Composable
fun TopPillNotification(controller: TopPillController) {
    val pillState by controller.pillState.collectAsStateWithLifecycle(initialValue = null)

    // Auto-dismiss timer
    LaunchedEffect(pillState?.id) {
        if (pillState != null) {
            delay(PILL_AUTO_DISMISS_MS)
            controller.dismiss()
        }
    }

    AnimatedVisibility(
        visible = pillState != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge, vertical = MaterialTheme.spacing.Small),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = PILL_SHAPE,
                color = MaterialTheme.colorScheme.inverseSurface,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = MaterialTheme.spacing.Large,
                        top = MaterialTheme.spacing.Small,
                        end = MaterialTheme.spacing.ExtraSmall,
                        bottom = MaterialTheme.spacing.Small
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BodyText(
                        text = pillState?.message.orEmpty(),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.ExtraSmall))
                    IconButton(
                        onClick = { controller.dismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.X,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
