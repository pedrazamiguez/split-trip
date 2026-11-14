package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel

private val OPTION_ICON_SIZE = 24.dp
private val CHECK_ICON_SIZE = 20.dp

private data class FundingSourceCardColors(
    val container: Color,
    val title: Color,
    val secondary: Color
)

private fun fundingSourceCardColors(
    isSelected: Boolean,
    colorScheme: ColorScheme
): FundingSourceCardColors = if (isSelected) {
    FundingSourceCardColors(
        container = colorScheme.primaryContainer,
        title = colorScheme.onPrimaryContainer,
        secondary = colorScheme.onPrimaryContainer
    )
} else {
    FundingSourceCardColors(
        container = colorScheme.surfaceContainerLow,
        title = colorScheme.onSurface,
        secondary = colorScheme.onSurfaceVariant
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
    val colors = fundingSourceCardColors(isSelected, MaterialTheme.colorScheme)

    FlatCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .semantics { selected = isSelected }
            .debouncedClickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = colors.container
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(OPTION_ICON_SIZE)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                CardSectionLabelText(text = option.displayText, color = colors.title)
                if (option.description.isNotEmpty()) {
                    SecondaryBodyText(
                        text = option.description,
                        color = colors.secondary,
                        maxLines = Int.MAX_VALUE
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = TablerIcons.Outline.Check,
                    contentDescription = null,
                    tint = colors.title,
                    modifier = Modifier.size(CHECK_ICON_SIZE)
                )
            }
        }
    }
}
