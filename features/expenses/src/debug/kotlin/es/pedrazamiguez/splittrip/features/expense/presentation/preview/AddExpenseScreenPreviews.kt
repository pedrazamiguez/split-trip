package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.SubunitOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubcategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.AddExpenseScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

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
    val previewCategories = ExpenseCategory.entries.map { category ->
        CategoryUiModel(
            id = category.name,
            displayText = stringResource(category.toStringRes()),
            icon = category.toIconVector()
        )
    }.toImmutableList()
    val previewSubcategories = ExpenseSubcategory.forCategory(ExpenseCategory.FOOD).map { subcategory ->
        SubcategoryUiModel(
            subcategory = subcategory,
            name = UiText.StringResource(subcategory.toStringRes()),
            icon = subcategory.toIconVector()
        )
    }.toImmutableList()

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
                availableCategories = previewCategories,
                selectedCategory = previewCategories.first { it.id == ExpenseCategory.FOOD.name },
                availableSubcategories = previewSubcategories,
                selectedSubcategory = previewSubcategories.first {
                    it.subcategory == ExpenseSubcategory.RESTAURANT
                },
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

@PreviewComplete
@Composable
private fun AddExpenseScreenContributionScopeStepPreview() {
    PreviewThemeWrapper {
        AddExpenseScreen(
            groupId = "group-1",
            uiState = AddExpenseUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                expenseTitle = "Dinner at Pad Thai place",
                sourceAmount = "80",
                availableCurrencies = PREVIEW_CURRENCIES,
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedFundingSource = FundingSourceUiModel(
                    id = PayerType.USER.name,
                    displayText = "Your Money"
                ),
                contributionScope = PayerType.USER,
                contributionSubunitOptions = persistentListOf(
                    SubunitOptionUiModel(id = "subunit-1", name = "Couple")
                ),
                currentStep = AddExpenseStep.CONTRIBUTION_SCOPE
            )
        )
    }
}
