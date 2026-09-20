package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PlaneTilt
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.balance.presentation.component.CategorySpendingItemRow
import es.pedrazamiguez.splittrip.features.balance.presentation.component.SubcategorySpendingItemRow
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategorySpendingUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SubcategorySpendingUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.CategorySpendingScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.CategorySpendingUiState
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_SUBCATEGORY_ITEM = SubcategorySpendingUiModel(
    subcategoryName = "Flights",
    subcategoryIcon = TablerIcons.Outline.PlaneTilt,
    formattedAmount = "150.00 €",
    percentageOfCategory = 100,
    rawAmountCents = 15000L
)

private val PREVIEW_CATEGORY_ITEM = CategorySpendingUiModel(
    categoryName = "Transport",
    categoryIcon = TablerIcons.Outline.PlaneTilt,
    formattedAmount = "150.00 €",
    progress = 0.75f,
    color = Color(0xFF4CAF50),
    subcategories = persistentListOf(PREVIEW_SUBCATEGORY_ITEM)
)

@PreviewComplete
@Composable
private fun CategorySpendingScreenLoadingPreview() {
    PreviewThemeWrapper {
        CategorySpendingScreen(
            uiState = CategorySpendingUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun CategorySpendingScreenEmptyPreview() {
    PreviewThemeWrapper {
        CategorySpendingScreen(
            uiState = CategorySpendingUiState(
                isLoading = false,
                items = persistentListOf()
            )
        )
    }
}

@PreviewComplete
@Composable
private fun CategorySpendingScreenWithDataPreview() {
    PreviewThemeWrapper {
        CategorySpendingScreen(
            uiState = CategorySpendingUiState(
                isLoading = false,
                items = persistentListOf(PREVIEW_CATEGORY_ITEM),
                totalFormattedAmount = "150.00 €"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun CategorySpendingItemRowPreview() {
    PreviewThemeWrapper {
        CategorySpendingItemRow(
            item = PREVIEW_CATEGORY_ITEM
        )
    }
}

@PreviewComplete
@Composable
private fun SubcategorySpendingItemRowPreview() {
    PreviewThemeWrapper {
        SubcategorySpendingItemRow(
            item = PREVIEW_SUBCATEGORY_ITEM
        )
    }
}
