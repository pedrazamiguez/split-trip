// SheetAction model + ActionBottomSheet composable are intentionally co-located
@file:Suppress("MatchingDeclarationName")

package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.DotsVertical

/**
 * Represents an action item in the ActionBottomSheet.
 *
 * @param text The display text for the action.
 * @param icon The icon to display for the action.
 * @param onClick Called when the action is selected.
 * @param enabled Whether the action is enabled. Defaults to true.
 * @param isDestructive Whether this is a destructive action (shown in error color). Defaults to false.
 */
@Immutable
data class SheetAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val isDestructive: Boolean = false
)

/**
 * A reusable Material 3 Action Bottom Sheet with a consistent header and dynamic actions.
 * Matches the visual style of CopyableTextSheet for a unified UX.
 *
 * @param title The title displayed below the header icon.
 * @param actions List of actions to display in the sheet.
 * @param onDismiss Called when the sheet should be dismissed.
 * @param icon The header icon. Defaults to a "more" icon.
 * @param modifier Modifier for the bottom sheet content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod") // Compose UI builder DSL
@Composable
fun ActionBottomSheet(
    title: String,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = TablerIcons.Outline.DotsVertical
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = MaterialTheme.spacing.Screen), // Essential for gesture navigation spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header section matching CopyableTextSheet style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.ExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
            ) {
                // Icon header with background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Actions list
            actions.forEachIndexed { index, action ->
                if (index > 0) {
                    HorizontalDivider()
                }

                val contentColor = when {
                    !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    action.isDestructive -> MaterialTheme.colorScheme.error
                    else -> Color.Unspecified
                }

                ListItem(
                    headlineContent = {
                        Text(
                            text = action.text,
                            color = contentColor
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = contentColor.takeIf { it != Color.Unspecified }
                                ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.clickable(enabled = action.enabled) {
                        action.onClick()
                    }
                )
            }
        }
    }
}
