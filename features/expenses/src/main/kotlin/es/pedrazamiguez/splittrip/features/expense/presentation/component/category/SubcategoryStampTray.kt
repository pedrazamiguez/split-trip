package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.features.expense.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SubcategoryStampTray(
    focusedCategoryId: String?,
    focusedCategoryLabel: String?,
    items: ImmutableList<SubcategoryStampItem>,
    onSubcategoryClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeStyle: PassportTagStyle = PassportTagStyle.INFO,
    badgeContentDescription: String? = null,
    containedInCard: Boolean = true
) {
    AnimatedVisibility(
        visible = items.isNotEmpty(),
        modifier = modifier,
        enter = CategorySelectorTransitions.trayEnterTransition(),
        exit = CategorySelectorTransitions.trayExitTransition()
    ) {
        AnimatedContent(
            targetState = SubcategoryTraySnapshot(
                categoryId = focusedCategoryId,
                categoryLabel = focusedCategoryLabel,
                items = items,
                badgeText = badgeText,
                badgeStyle = badgeStyle,
                badgeContentDescription = badgeContentDescription
            ),
            contentKey = { it.categoryId },
            transitionSpec = {
                with(CategorySelectorTransitions) {
                    trayContentTransform()
                }
            },
            label = "SubcategoryStampTrayTransition"
        ) { state ->
            val title = state.categoryLabel?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.add_expense_subcategory_title)
            SubcategoryStampTrayContent(
                title = title,
                items = state.items,
                onSubcategoryClicked = onSubcategoryClicked,
                modifier = Modifier.padding(top = MaterialTheme.spacing.Default),
                badgeText = state.badgeText,
                badgeStyle = state.badgeStyle,
                badgeContentDescription = state.badgeContentDescription,
                containedInCard = containedInCard
            )
        }
    }
}
