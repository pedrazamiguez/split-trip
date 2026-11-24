package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.component.filter.CategoryFilterSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.CategorySection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.chips.CondensedChips
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.chips.PaymentMethodChips
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.chips.PaymentStatusChips
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import kotlinx.collections.immutable.toImmutableList

private val PREVIEW_PAYMENT_CARD = PaymentMethodUiModel(
    id = "CREDIT_CARD",
    displayText = "Credit Card"
)

private val PREVIEW_PAYMENT_CASH = PaymentMethodUiModel(
    id = "CASH",
    displayText = "Cash"
)

private val PREVIEW_STATUS_FINISHED = PaymentStatusUiModel(
    id = "FINISHED",
    displayText = "Finished"
)

private val PREVIEW_STATUS_SCHEDULED = PaymentStatusUiModel(
    id = "SCHEDULED",
    displayText = "Scheduled"
)

@PreviewComplete
@Composable
private fun PaymentMethodChipsPreview() {
    PreviewThemeWrapper {
        PaymentMethodChips(
            paymentMethods = listOf(PREVIEW_PAYMENT_CARD, PREVIEW_PAYMENT_CASH),
            selectedPaymentMethod = PREVIEW_PAYMENT_CARD,
            onPaymentMethodSelected = {}
        )
    }
}

@PreviewComplete
@Composable
private fun PaymentStatusChipsPreview() {
    PreviewThemeWrapper {
        PaymentStatusChips(
            paymentStatuses = listOf(PREVIEW_STATUS_FINISHED, PREVIEW_STATUS_SCHEDULED),
            selectedPaymentStatus = PREVIEW_STATUS_FINISHED,
            onPaymentStatusSelected = {}
        )
    }
}

@PreviewComplete
@Composable
private fun CondensedChipsPreview() {
    PreviewThemeWrapper {
        CondensedChips(
            items = listOf("Flights", "Hotels", "Car Rental", "Activities", "Food"),
            selectedId = "Hotels",
            onItemSelected = {},
            itemId = { it },
            itemLabel = { it }
        )
    }
}

@Composable
private fun CategorySectionCarouselPreviewContent() {
    val categories = ExpenseCategory.entries.map { category ->
        CategoryUiModel(
            id = category.name,
            displayText = stringResource(category.toStringRes()),
            icon = category.toIconVector()
        )
    }.toImmutableList()
    val subcategories = ExpenseSubcategory.forCategory(ExpenseCategory.TRANSPORT).map { subcategory ->
        SubcategoryUiModel(
            subcategory = subcategory,
            name = UiText.StringResource(subcategory.toStringRes()),
            icon = subcategory.toIconVector()
        )
    }.toImmutableList()

    PreviewThemeWrapper {
        CategorySection(
            availableCategories = categories,
            selectedCategory = categories.first { it.id == ExpenseCategory.TRANSPORT.name },
            onCategorySelected = {},
            availableSubcategories = subcategories,
            selectedSubcategory = subcategories.first {
                it.subcategory == ExpenseSubcategory.INTERNATIONAL_FLIGHT
            },
            onSubcategorySelected = {},
            showAllCategories = false,
            onToggleShowAllCategories = {}
        )
    }
}

@PreviewComplete
@Composable
private fun CategorySectionCarouselPreview() {
    CategorySectionCarouselPreviewContent()
}

@PreviewNarrowThemes
@Composable
private fun CategorySectionCarouselNarrowPreview() {
    CategorySectionCarouselPreviewContent()
}

@Composable
private fun CategoryFilterSectionCarouselPreviewContent() {
    PreviewThemeWrapper {
        CategoryFilterSection(
            criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.TRANSPORT, ExpenseCategory.FOOD),
                selectedSubcategories = setOf(
                    ExpenseSubcategory.TAXI_RIDESHARE,
                    ExpenseSubcategory.TRAIN,
                    ExpenseSubcategory.RESTAURANT
                )
            ),
            onCriteriaChange = {}
        )
    }
}

@PreviewComplete
@Composable
private fun CategoryFilterSectionCarouselPreview() {
    CategoryFilterSectionCarouselPreviewContent()
}

@PreviewNarrowThemes
@Composable
private fun CategoryFilterSectionCarouselNarrowPreview() {
    CategoryFilterSectionCarouselPreviewContent()
}
