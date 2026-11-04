package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.AddExpenseScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_CURRENCIES = persistentListOf(
    CurrencyUiModel("EUR", "EUR - Euro", 2),
    CurrencyUiModel("THB", "THB - Thai Baht", 2),
    CurrencyUiModel("USD", "USD - US Dollar", 2)
)

@PreviewComplete
@Composable
private fun AddExpenseScreenLoadingPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            uiState = AddExpenseUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenConfigFailedPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                configLoadFailed = true
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenReadyPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2)
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenFilledPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner at Pad Thai place",
                sourceAmount = "450",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                showExchangeRateSection = true,
                displayExchangeRate = "37.037",
                calculatedGroupAmount = "12.15",
                exchangeRateLabel = "1 EUR = X THB",
                groupAmountLabel = "Cost in EUR"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenOnTopAddOnPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner",
                sourceAmount = "80",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                addOns = persistentListOf(
                    AddOnUiModel(
                        id = "preview-1",
                        type = AddOnType.TIP,
                        mode = AddOnMode.ON_TOP,
                        valueType = AddOnValueType.PERCENTAGE,
                        amountInput = "20"
                    )
                ),
                effectiveTotal = "€96.00"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenIncludedAddOnPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner with tip included",
                sourceAmount = "80",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                addOns = persistentListOf(
                    AddOnUiModel(
                        id = "preview-2",
                        type = AddOnType.TIP,
                        mode = AddOnMode.INCLUDED,
                        valueType = AddOnValueType.PERCENTAGE,
                        amountInput = "20"
                    )
                ),
                includedBaseCost = "€66.67"
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenExchangeRateStepPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner at Pad Thai place",
                sourceAmount = "450",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                showExchangeRateSection = true,
                displayExchangeRate = "37.037",
                calculatedGroupAmount = "12.15",
                exchangeRateLabel = "1 EUR = X THB",
                groupAmountLabel = "Cost in EUR",
                currentStep = AddExpenseStep.EXCHANGE_RATE
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenDetailsStepPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner",
                sourceAmount = "80",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                currentStep = AddExpenseStep.CATEGORY
            )
        )
    }
}

@PreviewComplete
@Composable
private fun AddExpenseScreenReviewStepPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner at Pad Thai place",
                sourceAmount = "450",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                showExchangeRateSection = true,
                displayExchangeRate = "37.037",
                calculatedGroupAmount = "12.15",
                exchangeRateLabel = "1 EUR = X THB",
                groupAmountLabel = "Cost in EUR",
                currentStep = AddExpenseStep.REVIEW
            )
        )
    }
}
