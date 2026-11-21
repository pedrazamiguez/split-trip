package es.pedrazamiguez.splittrip.features.expense.presentation.component.detail

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.HorizonModalBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.parseAmountToSmallestUnit
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmPaymentBottomSheet(
    expense: ExpenseDetailUiModel,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val validateAndConfirm: () -> Unit = {
        val parsed = runCatching {
            parseAmountToSmallestUnit(amountText, expense.groupCurrency)
        }.getOrNull()
        if (parsed != null && parsed > 0) {
            onConfirm(parsed)
        } else {
            isError = true
        }
    }

    HorizonModalBottomSheet(onDismissRequest = onDismiss) {
        ConfirmPaymentSheetContent(
            expense = expense,
            amountText = amountText,
            isError = isError,
            onAmountChange = {
                amountText = it
                isError = false
            },
            onConfirmClicked = validateAndConfirm
        )
    }
}
