package es.pedrazamiguez.splittrip.features.settings.presentation.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText

private val CHECK_BADGE_SIZE = 28.dp
private val CHECK_ICON_SIZE = 16.dp
private val TEXT_LINE_SPACING = 2.dp
private const val SELECTED_ROW_CONTAINER_ALPHA = 0.28f
private const val SELECTED_SUBTITLE_ALPHA = 0.8f

private data class SelectionRowColors(
    val rowContainer: Color,
    val leadingContainer: Color,
    val leadingContent: Color,
    val title: Color,
    val subtitle: Color
)

private fun resolveSelectionRowColors(
    isSelected: Boolean,
    colors: ColorScheme
): SelectionRowColors = if (isSelected) {
    SelectionRowColors(
        rowContainer = colors.primaryContainer.copy(alpha = SELECTED_ROW_CONTAINER_ALPHA),
        leadingContainer = colors.primaryContainer,
        leadingContent = colors.onPrimaryContainer,
        title = colors.primary,
        subtitle = colors.primary.copy(alpha = SELECTED_SUBTITLE_ALPHA)
    )
} else {
    SelectionRowColors(
        rowContainer = Color.Transparent,
        leadingContainer = colors.surfaceContainerHigh,
        leadingContent = colors.onSurfaceVariant,
        title = Color.Unspecified,
        subtitle = Color.Unspecified
    )
}

@Composable
internal fun SettingsSelectionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingBadgeText: String? = null
) {
    val rowColors = resolveSelectionRowColors(isSelected = isSelected, colors = MaterialTheme.colorScheme)
    val hasLeadingSlot = leadingIcon != null || !leadingBadgeText.isNullOrBlank()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(rowColors.rowContainer)
            .semantics {
                role = Role.RadioButton
                selected = isSelected
            }
            .debouncedClickable(onClick = onClick)
            .padding(MaterialTheme.spacing.Medium)
    ) {
        if (hasLeadingSlot) {
            SettingsSelectionLeadingBadge(
                leadingIcon = leadingIcon,
                leadingBadgeText = leadingBadgeText,
                containerColor = rowColors.leadingContainer,
                contentColor = rowColors.leadingContent
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(TEXT_LINE_SPACING)
        ) {
            CardTitleText(text = title, color = rowColors.title)
            SecondaryBodyText(text = subtitle, color = rowColors.subtitle)
        }

        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(CHECK_BADGE_SIZE)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.Check,
                    contentDescription = stringResource(DesignSystemR.string.content_description_selected),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(CHECK_ICON_SIZE)
                )
            }
        }
    }
}
