package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.features.expense.presentation.component.detail.ConfirmPaymentBottomSheet

@PreviewComplete
@Composable
private fun ConfirmPaymentBottomSheetCashPreview() {
    ExpenseDetailPreviewHelper(domainExpense = PREVIEW_EXPENSE_DETAIL_CASH_FIFO) {
        ConfirmPaymentBottomSheet(
            expense = it,
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ConfirmPaymentBottomSheetTransferPreview() {
    ExpenseDetailPreviewHelper(domainExpense = PREVIEW_EXPENSE_DETAIL_VANILLA) {
        ConfirmPaymentBottomSheet(
            expense = it,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
