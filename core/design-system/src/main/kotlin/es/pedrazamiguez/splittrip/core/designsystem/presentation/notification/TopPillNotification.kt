package es.pedrazamiguez.splittrip.core.designsystem.presentation.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.R
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

const val TOP_PILL_NOTIFICATION_TEST_TAG = "top_pill_notification"
const val TOP_PILL_LEADING_ICON_TEST_TAG = "top_pill_leading_icon"
const val TOP_PILL_MESSAGE_TEST_TAG = "top_pill_message"
const val TOP_PILL_DISMISS_BUTTON_TEST_TAG = "top_pill_dismiss_button"

/**
 * State for a single pill notification.
 */
data class PillState(
    val message: String,
    val style: TopPillStyle = TopPillStyle.NEUTRAL,
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
    fun showPill(
        message: String,
        style: TopPillStyle = TopPillStyle.NEUTRAL
    ) {
        scope.launch {
            _pillState.value = PillState(message = message, style = style)
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
 * pillController.showPill(message = action.message.asString(context), style = TopPillStyle.SUCCESS)
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

    // Cache the last non-null message and style so exit animations stay consistent until faded out.
    var displayedMessage by remember { mutableStateOf("") }
    var displayedStyle by remember { mutableStateOf(TopPillStyle.NEUTRAL) }
    pillState?.let {
        displayedMessage = it.message
        displayedStyle = it.style
    }

    // Auto-dismiss timer
    LaunchedEffect(pillState?.id) {
        if (pillState != null) {
            delay(PILL_AUTO_DISMISS_MS)
            controller.dismiss()
        }
    }

    AnimatedVisibility(
        visible = pillState != null,
        enter = TopPillDefaults.EnterTransition,
        exit = TopPillDefaults.ExitTransition
    ) {
        TopPillNotificationContent(
            message = displayedMessage,
            onDismiss = { controller.dismiss() },
            style = displayedStyle
        )
    }
}

@Composable
internal fun TopPillNotificationContent(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    style: TopPillStyle = TopPillStyle.NEUTRAL
) {
    val colors = TopPillDefaults.colors(style)
    val leadingIcon = TopPillDefaults.icon(style)

    TopNotificationContainer(
        style = style,
        modifier = modifier,
        surfaceModifier = Modifier.testTag(TOP_PILL_NOTIFICATION_TEST_TAG)
    ) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.Default,
                top = MaterialTheme.spacing.Small,
                end = MaterialTheme.spacing.ExtraSmall,
                bottom = MaterialTheme.spacing.Small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.iconColor,
                    modifier = Modifier
                        .size(TopPillDefaults.LeadingIconSize)
                        .testTag(TOP_PILL_LEADING_ICON_TEST_TAG)
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.Medium))
            }
            BodyText(
                text = message,
                color = colors.contentColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = MaterialTheme.spacing.ExtraSmall)
                    .testTag(TOP_PILL_MESSAGE_TEST_TAG)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.ExtraSmall))
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TOP_PILL_DISMISS_BUTTON_TEST_TAG)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.content_description_dismiss),
                    tint = colors.contentColor,
                    modifier = Modifier.size(TopPillDefaults.DismissIconSize)
                )
            }
        }
    }
}
