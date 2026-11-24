package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardTitleText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryGridSelector
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridEnterTransition
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridExitTransition
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun CategoryFilterSubcategorySection(
    category: ExpenseCategory,
    criteria: ExpenseFilterCriteria,
    groupContext: GroupContext,
    showAllCategories: Boolean,
    onSubcategoryToggled: (ExpenseSubcategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val subcategories = remember(category, groupContext, showAllCategories, criteria.selectedSubcategories) {
        resolveVisibleSubcategories(category, criteria.selectedSubcategories, groupContext, showAllCategories)
    }

    AnimatedVisibility(
        visible = category in criteria.selectedCategories && subcategories.isNotEmpty(),
        modifier = modifier,
        enter = subcategoryGridEnterTransition(),
        exit = subcategoryGridExitTransition()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            CardTitleText(
                text = stringResource(category.toStringRes()),
                color = MaterialTheme.colorScheme.primary
            )

            CategoryGridSelector(
                items = subcategories,
                isItemSelected = { it in criteria.selectedSubcategories },
                onItemClicked = onSubcategoryToggled,
                itemId = { it.name },
                itemLabel = { stringResource(it.toStringRes()) },
                itemIcon = { it.toIconVector() }
            )
        }
    }
}

private fun resolveVisibleSubcategories(
    category: ExpenseCategory,
    selectedSubcategories: Set<ExpenseSubcategory>,
    groupContext: GroupContext,
    showAllCategories: Boolean
): ImmutableList<ExpenseSubcategory> {
    if (showAllCategories || !category.appliesTo(groupContext)) {
        return ExpenseSubcategory.forCategory(category).toImmutableList()
    }
    val base = ExpenseSubcategory.forCategory(category, groupContext)
    val selectedForCategory = selectedSubcategories
        .filter { it.parentCategory == category && it !in base }
    return (base + selectedForCategory).distinct().toImmutableList()
}
