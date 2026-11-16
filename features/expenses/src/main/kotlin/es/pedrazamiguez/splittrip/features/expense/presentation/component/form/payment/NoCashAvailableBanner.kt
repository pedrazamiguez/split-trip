package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.features.expense.R

/**
 * Inline error banner shown when no ATM cash withdrawals exist at all
 * for the selected currency and cash pool.
 */
@Composable
fun NoCashAvailableBanner(
    currencyCode: String?,
    modifier: Modifier = Modifier
) {
    val errorText = if (!currencyCode.isNullOrBlank()) {
        UiText.StringResource(R.string.add_expense_cash_no_cash_available, currencyCode)
    } else {
        UiText.StringResource(R.string.add_expense_cash_no_cash_available_generic)
    }
    FormErrorBanner(error = errorText, modifier = modifier)
}
