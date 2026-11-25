package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText

@Composable
internal fun SubcategoryTrayHeaderRow(
    title: String,
    showBadge: Boolean,
    retainedBadgeText: String,
    badgeStyle: PassportTagStyle,
    badgeModifier: Modifier,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CardSectionLabelText(text = title, modifier = Modifier.weight(1f), maxLines = 1)
        AnimatedVisibility(
            visible = showBadge,
            enter = CategorySelectorTransitions.badgeEnterTransition(),
            exit = CategorySelectorTransitions.badgeExitTransition()
        ) {
            AnimatedContent(
                targetState = retainedBadgeText,
                transitionSpec = {
                    with(CategorySelectorTransitions) { badgeCountTransform() }
                },
                modifier = Modifier.padding(start = MaterialTheme.spacing.Small),
                label = "SubcategoryTrayBadgeCount"
            ) { text ->
                PassportTag(text = text, style = badgeStyle, modifier = badgeModifier)
            }
        }
    }
}
