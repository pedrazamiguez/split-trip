package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpenseDetailScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpenseDetailUiState

@PreviewComplete
@Composable
private fun ExpenseDetailScreenLoadingPreview() {
    PreviewThemeWrapper {
        ExpenseDetailScreen(uiState = ExpenseDetailUiState(isLoading = true))
    }
}

@PreviewComplete
@Composable
private fun ExpenseDetailScreenErrorPreview() {
    PreviewThemeWrapper {
        ExpenseDetailScreen(uiState = ExpenseDetailUiState(isLoading = false, hasError = true))
    }
}

@PreviewComplete
@Composable
private fun ExpenseDetailScreenVanillaPreview() {
    ExpenseDetailPreviewHelper(domainExpense = PREVIEW_EXPENSE_DETAIL_VANILLA) { uiModel ->
        ExpenseDetailScreen(uiState = ExpenseDetailUiState(expense = uiModel, isLoading = false))
    }
}

@PreviewComplete
@Composable
private fun ExpenseDetailScreenIncludedTipPreview() {
    ExpenseDetailPreviewHelper(
        domainExpense = PREVIEW_EXPENSE_DETAIL_INCLUDED_TIP,
        subunitNameLookup = PREVIEW_DETAIL_SUBUNIT_NAMES
    ) { uiModel ->
        ExpenseDetailScreen(uiState = ExpenseDetailUiState(expense = uiModel, isLoading = false))
    }
}

@PreviewComplete
@Composable
private fun ExpenseDetailScreenCashFifoPreview() {
    ExpenseDetailPreviewHelper(
        domainExpense = PREVIEW_EXPENSE_DETAIL_CASH_FIFO,
        withdrawalLookup = PREVIEW_DETAIL_WITHDRAWAL_LOOKUP
    ) { uiModel ->
        ExpenseDetailScreen(uiState = ExpenseDetailUiState(expense = uiModel, isLoading = false))
    }
}

@PreviewComplete
@Composable
private fun ExpenseDetailScreenForeignAddOnsPreview() {
    ExpenseDetailPreviewHelper(domainExpense = PREVIEW_EXPENSE_DETAIL_FOREIGN_ADDONS) { uiModel ->
        ExpenseDetailScreen(uiState = ExpenseDetailUiState(expense = uiModel, isLoading = false))
    }
}
