package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.SubcategoryStampItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.SubcategoryStampTray
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun CategoryFilterSubcategorySection(
    focusedCategory: ExpenseCategory?,
    availableSubcategories: ImmutableList<ExpenseSubcategory>,
    criteria: ExpenseFilterCriteria,
    onSubcategoryToggled: (ExpenseSubcategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val subcategories = remember(
        focusedCategory,
        availableSubcategories,
        criteria.selectedSubcategories
    ) {
        focusedCategory?.let { category ->
            CategoryFilterLogic.resolveVisibleSubcategories(
                category = category,
                availableSubcategories = availableSubcategories,
                selectedSubcategories = criteria.selectedSubcategories
            )
        } ?: persistentListOf()
    }

    val stampItems = subcategories.map { subcategory ->
        SubcategoryStampItem(
            id = subcategory.name,
            label = stringResource(subcategory.toStringRes()),
            icon = subcategory.toIconVector(),
            isSelected = subcategory in criteria.selectedSubcategories
        )
    }.toImmutableList()

    val selectedCount = focusedCategory?.let {
        CategoryFilterLogic.countSelectedSubcategories(criteria, it)
    } ?: 0

    val badgeContentDescription = if (selectedCount > 0) {
        pluralStringResource(R.plurals.category_filter_selected_count, selectedCount, selectedCount)
    } else {
        null
    }

    SubcategoryStampTray(
        focusedCategoryId = focusedCategory?.name,
        focusedCategoryLabel = focusedCategory?.let { stringResource(it.toStringRes()) },
        items = stampItems,
        onSubcategoryClicked = { id ->
            subcategories.find { it.name == id }?.let(onSubcategoryToggled)
        },
        modifier = modifier,
        badgeText = if (selectedCount > 0) selectedCount.toString() else null,
        badgeStyle = PassportTagStyle.INFO,
        badgeContentDescription = badgeContentDescription
    )
}
