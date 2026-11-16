package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment

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
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel

private val ICON_CONTAINER_SIZE = 40.dp
private val OPTION_ICON_SIZE = 20.dp
private val CHECK_ICON_SIZE = 20.dp
private const val COLOR_ANIMATION_DURATION_MS = 200
private const val TITLE_MAX_LINES = 2

private data class FundingSourceCardColors(
    val container: Color,
    val iconContainer: Color,
    val iconTint: Color,
    val title: Color,
    val description: Color,
    val checkTint: Color
)

private fun fundingSourceCardColors(
    isSelected: Boolean,
    colorScheme: ColorScheme
): FundingSourceCardColors = if (isSelected) {
    FundingSourceCardColors(
        container = colorScheme.surfaceContainerHighest,
        iconContainer = colorScheme.primaryContainer,
        iconTint = colorScheme.onPrimaryContainer,
        title = colorScheme.primary,
        description = colorScheme.onSurfaceVariant,
        checkTint = colorScheme.primary
    )
} else {
    FundingSourceCardColors(
        container = colorScheme.surfaceContainerLow,
        iconContainer = colorScheme.surfaceContainerHigh,
        iconTint = colorScheme.onSurfaceVariant,
        title = colorScheme.onSurface,
        description = colorScheme.onSurfaceVariant,
        checkTint = colorScheme.primary
    )
}

/**
 * Selectable option card for choosing a funding source ("Group Pocket" or "Your Money").
 * Always displays the option's plain-language description alongside its title and icon
 * so first-time users understand the choice before tapping.
 */
@Composable
internal fun FundingSourceOptionCard(
    option: FundingSourceUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = MaterialTheme.shapes.medium
    val targetColors = fundingSourceCardColors(isSelected, MaterialTheme.colorScheme)
    val colorSpec = remember { tween<Color>(durationMillis = COLOR_ANIMATION_DURATION_MS) }
    val containerColor by animateColorAsState(targetColors.container, colorSpec, label = "container")
    val iconContainerColor by animateColorAsState(targetColors.iconContainer, colorSpec, label = "iconContainer")
    val iconTint by animateColorAsState(targetColors.iconTint, colorSpec, label = "iconTint")
    val titleColor by animateColorAsState(targetColors.title, colorSpec, label = "title")
    val descriptionColor by animateColorAsState(targetColors.description, colorSpec, label = "description")
    val checkTransition = updateTransition(targetState = isSelected, label = "checkVisibility")

    FlatCard(
        modifier = modifier.fillMaxWidth().clip(cardShape)
            .semantics { selected = isSelected }
            .debouncedClickable(role = Role.RadioButton, onClick = onClick),
        shape = cardShape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.Default),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(ICON_CONTAINER_SIZE).clip(CircleShape).background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(OPTION_ICON_SIZE)
                )
            }
            Column(Modifier.weight(1f), Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
                CardTitleText(text = option.displayText, color = titleColor, maxLines = TITLE_MAX_LINES)
                if (option.description.isNotEmpty()) {
                    SecondaryBodyText(option.description, color = descriptionColor, maxLines = Int.MAX_VALUE)
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
                        tint = targetColors.checkTint,
                        modifier = Modifier.size(CHECK_ICON_SIZE)
                    )
                }
            }
        }
    }
}
