package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.domain.enums.ExpenseAttribute
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesFilterScreen
import java.time.LocalDate

@PreviewComplete
@Composable
private fun ExpensesFilterScreenDefaultPreview() {
    ExpensesFilterPreviewHelper { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenSingleCategoryPreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.FOOD)
        ),
        matchingExpensesCount = 4
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenMultipleCategoriesPreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            selectedCategories = setOf(ExpenseCategory.FOOD, ExpenseCategory.TRANSPORT),
            selectedSubcategories = setOf(
                ExpenseSubcategory.RESTAURANT,
                ExpenseSubcategory.TAXI_RIDESHARE
            )
        ),
        matchingExpensesCount = 2
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenMemberFilterPreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            selectedMemberIds = setOf("user-1", "user-2")
        ),
        matchingExpensesCount = 3
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenDateRangePreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 15)
        ),
        matchingExpensesCount = 5
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenPresetDatePreview() {
    val today = LocalDate.now()
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            startDate = today,
            endDate = today
        ),
        matchingExpensesCount = 3,
        today = today
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenPaymentMethodPreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            selectedPaymentMethods = setOf(PaymentMethod.ALIPAY, PaymentMethod.CASH)
        ),
        matchingExpensesCount = 2
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenPaymentStatusPreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            selectedPaymentStatuses = setOf(PaymentStatus.SCHEDULED, PaymentStatus.REFUNDABLE)
        ),
        matchingExpensesCount = 2
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}

@PreviewComplete
@Composable
private fun ExpensesFilterScreenAttributesPreview() {
    ExpensesFilterPreviewHelper(
        draftCriteria = ExpenseFilterCriteria(
            selectedAttributes = setOf(ExpenseAttribute.HAS_RECEIPT, ExpenseAttribute.HAS_NOTE)
        ),
        matchingExpensesCount = 2
    ) { uiState ->
        ExpensesFilterScreen(uiState = uiState)
    }
}
