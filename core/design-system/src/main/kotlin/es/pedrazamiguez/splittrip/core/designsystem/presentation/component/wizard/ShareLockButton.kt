package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.LockFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LockOpen

private val STANDARD_BUTTON_SIZE = 40.dp
private val NARROW_BUTTON_SIZE = 36.dp
private val ICON_SIZE = 18.dp
private const val UNLOCKED_ICON_ALPHA = 0.6f

/**
 * Shared lock/unlock toggle button for participant share-allocation rows across
 * `:features:expenses` (`SplitMemberRow`, `EntitySplitRow`, `IntraSubunitSplitEditor`)
 * and `:features:subunits` (`SubunitSharesStep`).
 *
 * Uses a `40.dp` button target on standard screens (`> 360dp`) and `36.dp` on narrow screens
 * (`<= 360dp`) so adjacent input fields and switches retain breathing room.
 */
@Composable
fun ShareLockButton(
    isLocked: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNarrowScreen = LocalConfiguration.current.screenWidthDp.dp <= UiConstants.NARROW_SCREEN_BREAKPOINT
    val buttonSize = if (isNarrowScreen) NARROW_BUTTON_SIZE else STANDARD_BUTTON_SIZE

    IconButton(
        onClick = onClick,
        modifier = modifier.size(buttonSize)
    ) {
        Icon(
            imageVector = if (isLocked) TablerIcons.Filled.LockFilled else TablerIcons.Outline.LockOpen,
            contentDescription = contentDescription,
            tint = if (isLocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNLOCKED_ICON_ALPHA)
            },
            modifier = Modifier.size(ICON_SIZE)
        )
    }
}
