package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.CategoryGridSelector
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridContentTransform
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridEnterTransition
import es.pedrazamiguez.splittrip.features.expense.presentation.component.category.subcategoryGridExitTransition
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import kotlinx.collections.immutable.ImmutableList

/**
 * Category selection using a fixed-position grid with optional subcategory selection.
 */
@Composable
internal fun CategorySection(
    availableCategories: ImmutableList<CategoryUiModel>,
    selectedCategory: CategoryUiModel?,
    onCategorySelected: (String) -> Unit,
    availableSubcategories: ImmutableList<SubcategoryUiModel>,
    selectedSubcategory: SubcategoryUiModel?,
    onSubcategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            SectionHeadingText(text = stringResource(R.string.add_expense_category_title))

            CategoryGridSelector(
                items = availableCategories,
                isItemSelected = { it.id == selectedCategory?.id },
                onItemClicked = { category ->
                    onCategorySelected(category.id)
                    focusManager.clearFocus()
                },
                itemId = { it.id },
                itemLabel = { it.displayText },
                itemIcon = { it.icon }
            )
        }

        AnimatedVisibility(
            visible = availableSubcategories.isNotEmpty(),
            enter = subcategoryGridEnterTransition(),
            exit = subcategoryGridExitTransition()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.Large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
            ) {
                SectionHeadingText(text = stringResource(R.string.add_expense_subcategory_title))

                AnimatedContent(
                    targetState = selectedCategory?.id to availableSubcategories,
                    contentKey = { (categoryId, _) -> categoryId },
                    transitionSpec = { subcategoryGridContentTransform() },
                    label = "SubcategoryGridTransition"
                ) { (_, subcategories) ->
                    CategoryGridSelector(
                        items = subcategories,
                        isItemSelected = { it.subcategory == selectedSubcategory?.subcategory },
                        onItemClicked = { subcategory ->
                            val id = subcategory.subcategory.name
                            onSubcategorySelected(if (selectedSubcategory?.subcategory?.name == id) null else id)
                            focusManager.clearFocus()
                        },
                        itemId = { it.subcategory.name },
                        itemLabel = { it.name.asString() },
                        itemIcon = { it.icon }
                    )
                }
            }
        }
    }
}
