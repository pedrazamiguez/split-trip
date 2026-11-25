package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryCarousel
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryCarouselItem
import kotlinx.collections.immutable.toImmutableList

@Composable
fun CategoryFilterSection(
    criteria: ExpenseFilterCriteria,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier,
    groupContext: GroupContext = GroupContext.TRAVEL,
    showAllCategories: Boolean = false,
    onToggleShowAllCategories: () -> Unit = {},
    carouselEdgePadding: Dp = 0.dp
) {
    val visible = remember(groupContext, showAllCategories, criteria.selectedCategories) {
        CategoryFilterLogic.resolveVisibleCategories(groupContext, showAllCategories, criteria.selectedCategories)
    }
    var focusedCategory by rememberSaveable { mutableStateOf<ExpenseCategory?>(null) }
    val effectiveFocused = CategoryFilterLogic.resolveFocusedCategory(
        focusedCategory,
        criteria.selectedCategories,
        visible
    )
    LaunchedEffect(effectiveFocused) { focusedCategory = effectiveFocused }
    val carouselItems = visible.map { category ->
        CategoryCarouselItem(
            id = category.name,
            label = stringResource(category.toStringRes()),
            icon = category.toIconVector(),
            isSelected = category in criteria.selectedCategories,
            isFocused = category == effectiveFocused,
            activeSubcategoryCount = CategoryFilterLogic.countSelectedSubcategories(criteria, category)
        )
    }.toImmutableList()

    val count = criteria.selectedCategories.size
    var retainedCount by remember { mutableIntStateOf(count) }
    if (count > 0) {
        retainedCount = count
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            CategoryFilterHeader(
                selectedCount = count,
                retainedCount = retainedCount,
                showAllCategories = showAllCategories,
                onToggleShowAllCategories = onToggleShowAllCategories
            )
            CategoryCarousel(
                items = carouselItems,
                focusedItemId = effectiveFocused?.name,
                onItemClicked = { id ->
                    val clicked = visible.find { it.name == id } ?: return@CategoryCarousel
                    val res = CategoryFilterLogic.handleCategoryClick(criteria, clicked, effectiveFocused, visible)
                    focusedCategory = res.nextFocusedCategory
                    if (res.updatedCriteria != criteria) onCriteriaChange(res.updatedCriteria)
                },
                edgePadding = carouselEdgePadding,
                selectionRole = Role.Checkbox
            )
        }
        CategoryFilterSubcategorySection(effectiveFocused, criteria, groupContext, showAllCategories, {
            onCriteriaChange(CategoryFilterLogic.toggleSubcategory(criteria, it))
        })
    }
}
