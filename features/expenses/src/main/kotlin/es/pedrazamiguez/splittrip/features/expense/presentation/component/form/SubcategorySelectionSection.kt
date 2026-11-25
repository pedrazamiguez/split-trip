package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.SubcategoryStampItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.SubcategoryStampTray
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun SubcategorySelectionSection(
    selectedCategoryId: String?,
    selectedCategoryLabel: String?,
    availableSubcategories: ImmutableList<SubcategoryUiModel>,
    selectedSubcategory: SubcategoryUiModel?,
    onSubcategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val stampItems = availableSubcategories.map { subcategory ->
        SubcategoryStampItem(
            id = subcategory.subcategory.name,
            label = subcategory.name.asString(),
            icon = subcategory.icon,
            isSelected = subcategory.subcategory == selectedSubcategory?.subcategory
        )
    }.toImmutableList()

    SubcategoryStampTray(
        focusedCategoryId = selectedCategoryId,
        focusedCategoryLabel = selectedCategoryLabel,
        items = stampItems,
        onSubcategoryClicked = { id ->
            onSubcategorySelected(if (selectedSubcategory?.subcategory?.name == id) null else id)
        },
        modifier = modifier,
        useSectionHeading = true
    )
}
