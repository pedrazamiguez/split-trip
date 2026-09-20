package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon.AddOnItemEditor
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon.AddOnsSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_CURRENCY_EUR = CurrencyUiModel(
    code = "EUR",
    displayText = "EUR (€)",
    decimalDigits = 2,
    defaultName = "Euro",
    localizedName = "Euro"
)

private val PREVIEW_CURRENCY_USD = CurrencyUiModel(
    code = "USD",
    displayText = "USD ($)",
    decimalDigits = 2,
    defaultName = "US Dollar",
    localizedName = "US Dollar"
)

private val PREVIEW_PAYMENT_METHOD_CARD = PaymentMethodUiModel(
    id = "CREDIT_CARD",
    displayText = "Credit Card"
)

private val PREVIEW_PAYMENT_METHOD_CASH = PaymentMethodUiModel(
    id = "CASH",
    displayText = "Cash"
)

private val PREVIEW_ADDON_PERCENTAGE = AddOnUiModel(
    id = "addon-1",
    type = AddOnType.TIP,
    mode = AddOnMode.ON_TOP,
    valueType = AddOnValueType.PERCENTAGE,
    amountInput = "10",
    currency = PREVIEW_CURRENCY_EUR,
    paymentMethod = PREVIEW_PAYMENT_METHOD_CARD
)

private val PREVIEW_ADDON_EXACT = AddOnUiModel(
    id = "addon-2",
    type = AddOnType.FEE,
    mode = AddOnMode.ON_TOP,
    valueType = AddOnValueType.EXACT,
    amountInput = "5.00",
    currency = PREVIEW_CURRENCY_EUR,
    paymentMethod = PREVIEW_PAYMENT_METHOD_CARD
)

@PreviewComplete
@Composable
private fun AddOnsSectionEmptyPreview() {
    PreviewThemeWrapper {
        AddOnsSection(
            uiState = AddExpenseUiState(
                addOns = persistentListOf(),
                isAddOnsSectionExpanded = false
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun AddOnsSectionWithItemsPreview() {
    PreviewThemeWrapper {
        AddOnsSection(
            uiState = AddExpenseUiState(
                addOns = persistentListOf(PREVIEW_ADDON_PERCENTAGE, PREVIEW_ADDON_EXACT),
                isAddOnsSectionExpanded = true,
                effectiveTotal = "55.00 €",
                includedBaseCost = "50.00 €",
                availableCurrencies = persistentListOf(PREVIEW_CURRENCY_EUR, PREVIEW_CURRENCY_USD),
                paymentMethods = persistentListOf(PREVIEW_PAYMENT_METHOD_CARD, PREVIEW_PAYMENT_METHOD_CASH),
                groupCurrency = PREVIEW_CURRENCY_EUR
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun AddOnItemEditorPercentagePreview() {
    PreviewThemeWrapper {
        AddOnItemEditor(
            addOn = PREVIEW_ADDON_PERCENTAGE,
            availableCurrencies = persistentListOf(PREVIEW_CURRENCY_EUR, PREVIEW_CURRENCY_USD),
            paymentMethods = persistentListOf(PREVIEW_PAYMENT_METHOD_CARD, PREVIEW_PAYMENT_METHOD_CASH),
            showCurrencySelector = true,
            onEvent = {},
            onRemove = {},
            groupCurrency = PREVIEW_CURRENCY_EUR
        )
    }
}

@PreviewComplete
@Composable
private fun AddOnItemEditorExactPreview() {
    PreviewThemeWrapper {
        AddOnItemEditor(
            addOn = PREVIEW_ADDON_EXACT,
            availableCurrencies = persistentListOf(PREVIEW_CURRENCY_EUR, PREVIEW_CURRENCY_USD),
            paymentMethods = persistentListOf(PREVIEW_PAYMENT_METHOD_CARD, PREVIEW_PAYMENT_METHOD_CASH),
            showCurrencySelector = true,
            onEvent = {},
            onRemove = {},
            groupCurrency = PREVIEW_CURRENCY_EUR
        )
    }
}
