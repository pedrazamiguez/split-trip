package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategorySelectorTransitions
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.ShowAllCategoriesChip

@Composable
internal fun CategoryFilterHeader(
    selectedCount: Int,
    retainedCount: Int,
    showAllCategories: Boolean,
    onToggleShowAllCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_category))
            AnimatedVisibility(
                visible = selectedCount > 0,
                enter = CategorySelectorTransitions.badgeEnterTransition(),
                exit = CategorySelectorTransitions.badgeExitTransition()
            ) {
                val desc = pluralStringResource(
                    R.plurals.category_filter_selected_count,
                    retainedCount,
                    retainedCount
                )
                AnimatedContent(
                    targetState = retainedCount,
                    transitionSpec = {
                        with(CategorySelectorTransitions) { badgeCountTransform() }
                    },
                    modifier = Modifier.padding(start = MaterialTheme.spacing.Small),
                    label = "CategoryFilterHeaderCount"
                ) { animatedCount ->
                    PassportTag(
                        text = animatedCount.toString(),
                        modifier = Modifier.semantics { contentDescription = desc },
                        style = PassportTagStyle.INFO
                    )
                }
            }
        }
        ShowAllCategoriesChip(showAllCategories, onToggleShowAllCategories)
    }
}
