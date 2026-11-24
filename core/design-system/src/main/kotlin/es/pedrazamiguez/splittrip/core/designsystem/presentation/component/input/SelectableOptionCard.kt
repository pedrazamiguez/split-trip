package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

private val ICON_CONTAINER_SIZE = 40.dp
private val OPTION_ICON_SIZE = 20.dp
private val CHECK_ICON_SIZE = 20.dp
private const val COLOR_ANIMATION_DURATION_MS = 200
private const val TITLE_MAX_LINES = 2
private const val SELECTED_CONTAINER_ALPHA = 0.12f

private data class SelectableOptionCardColors(
    val container: Color,
    val iconContainer: Color,
    val iconTint: Color,
    val title: Color,
    val description: Color,
    val checkTint: Color
)

private fun selectableOptionCardColors(
    isSelected: Boolean,
    colorScheme: ColorScheme
): SelectableOptionCardColors = if (isSelected) {
    SelectableOptionCardColors(
        container = colorScheme.primary.copy(alpha = SELECTED_CONTAINER_ALPHA).compositeOver(colorScheme.surface),
        iconContainer = colorScheme.primaryContainer,
        iconTint = colorScheme.onPrimaryContainer,
        title = colorScheme.primary,
        description = colorScheme.onSurfaceVariant,
        checkTint = colorScheme.primary
    )
} else {
    SelectableOptionCardColors(
        container = colorScheme.surfaceContainerLow,
        iconContainer = colorScheme.surfaceContainerHigh,
        iconTint = colorScheme.onSurfaceVariant,
        title = colorScheme.onSurface,
        description = colorScheme.onSurfaceVariant,
        checkTint = colorScheme.primary
    )
}

@Composable
private fun rememberAnimatedOptionCardColors(isSelected: Boolean): SelectableOptionCardColors {
    val targetColors = selectableOptionCardColors(isSelected, MaterialTheme.colorScheme)
    val colorSpec = remember { tween<Color>(durationMillis = COLOR_ANIMATION_DURATION_MS) }
    val container by animateColorAsState(targetColors.container, colorSpec, label = "container")
    val iconContainer by animateColorAsState(targetColors.iconContainer, colorSpec, label = "iconContainer")
    val iconTint by animateColorAsState(targetColors.iconTint, colorSpec, label = "iconTint")
    val title by animateColorAsState(targetColors.title, colorSpec, label = "title")
    val description by animateColorAsState(targetColors.description, colorSpec, label = "description")
    return SelectableOptionCardColors(
        container = container,
        iconContainer = iconContainer,
        iconTint = iconTint,
        title = title,
        description = description,
        checkTint = targetColors.checkTint
    )
}

/**
 * Horizon Narrative selectable option card displaying an icon badge, title, optional
 * always-visible multi-line description, and an animated trailing checkmark when selected.
 *
 * Used for wizard choice steps such as Expense Funding Source and Group Context.
 */
@Composable
fun SelectableOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = MaterialTheme.shapes.medium
    val colors = rememberAnimatedOptionCardColors(isSelected)
    val checkTransition = updateTransition(targetState = isSelected, label = "checkVisibility")

    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .semantics { selected = isSelected }
            .debouncedClickable(role = Role.RadioButton, onClick = onClick),
        shape = cardShape,
        color = colors.container
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(ICON_CONTAINER_SIZE)
                    .clip(CircleShape)
                    .background(colors.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(OPTION_ICON_SIZE)
                )
            }
            Column(Modifier.weight(1f), Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                CardTitleText(text = title, color = colors.title, maxLines = TITLE_MAX_LINES)
                if (description.isNotEmpty()) {
                    SecondaryBodyText(description, color = colors.description, maxLines = Int.MAX_VALUE)
                }
            }
            Box(modifier = Modifier.size(CHECK_ICON_SIZE), contentAlignment = Alignment.Center) {
                checkTransition.AnimatedVisibility(
                    visible = { it },
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = TablerIcons.Outline.Check,
                        contentDescription = null,
                        tint = colors.checkTint,
                        modifier = Modifier.size(CHECK_ICON_SIZE)
                    )
                }
            }
        }
    }
}
