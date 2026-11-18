package es.pedrazamiguez.splittrip.features.withdrawal.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen.AddCashWithdrawalScreen
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.CashWithdrawalStep
import kotlinx.collections.immutable.persistentListOf

private val PREVIEW_CURRENCIES = persistentListOf(
    CurrencyUiModel("EUR", "EUR - Euro", 2),
    CurrencyUiModel("THB", "THB - Thai Baht", 2),
    CurrencyUiModel("USD", "USD - US Dollar", 2)
)

private val PREVIEW_MEMBERS = persistentListOf(
    MemberOptionUiModel(userId = "user-1", displayName = "Andrés", isCurrentUser = true),
    MemberOptionUiModel(userId = "user-2", displayName = "Ana", isCurrentUser = false),
    MemberOptionUiModel(userId = "user-3", displayName = "Luis", isCurrentUser = false)
)

@PreviewComplete
@Composable
private fun AddCashWithdrawalScreenLoadingPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            uiState = AddCashWithdrawalUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun AddCashWithdrawalScreenConfigFailedPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                configLoadFailed = true
            )
        )
    }
}

// ── Step 1: Amount ────────────────────────────────────────────────────────────

@PreviewComplete
@Composable
private fun WizardAmountStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                currentStep = CashWithdrawalStep.AMOUNT
            )
        )
    }
}

// ── Step 2 (conditional): Exchange Rate ──────────────────────────────────────

@PreviewComplete
@Composable
private fun WizardExchangeRateStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "10000",
                showExchangeRateSection = true,
                exchangeRateLabel = "1 EUR = ? THB",
                deductedAmountLabel = "in EUR",
                displayExchangeRate = "37.037",
                deductedAmount = "270.00",
                currentStep = CashWithdrawalStep.EXCHANGE_RATE
            )
        )
    }
}

// ── Step 3: Scope ─────────────────────────────────────────────────────────────

@PreviewComplete
@Composable
private fun WizardScopeStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "200",
                withdrawalScope = PayerType.GROUP,
                groupMembers = PREVIEW_MEMBERS,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés",
                currentStep = CashWithdrawalStep.SCOPE
            )
        )
    }
}

@PreviewComplete
@Composable
private fun WizardScopeStepImpersonatedPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "200",
                withdrawalScope = PayerType.USER,
                groupMembers = PREVIEW_MEMBERS,
                selectedMemberId = "user-2",
                selectedMemberDisplayName = "Ana",
                currentStep = CashWithdrawalStep.SCOPE
            )
        )
    }
}

// ── Step 4: Details + ATM fee toggle ─────────────────────────────────────────

@PreviewComplete
@Composable
private fun WizardDetailsStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "200",
                withdrawalScope = PayerType.GROUP,
                currentStep = CashWithdrawalStep.DETAILS
            )
        )
    }
}

// ── Step 5 (conditional): ATM Fee amount ─────────────────────────────────────

@PreviewComplete
@Composable
private fun WizardAtmFeeStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "10000",
                showExchangeRateSection = true,
                hasFee = true,
                feeAmount = "220",
                feeCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                currentStep = CashWithdrawalStep.ATM_FEE
            )
        )
    }
}

// ── Step 6 (conditional): Fee Exchange Rate ───────────────────────────────────

@PreviewComplete
@Composable
private fun WizardFeeExchangeRateStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "10000",
                showExchangeRateSection = true,
                hasFee = true,
                feeAmount = "220",
                feeCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                showFeeExchangeRateSection = true,
                feeExchangeRate = "37.037",
                feeExchangeRateLabel = "1 EUR = ? THB",
                feeConvertedLabel = "in EUR",
                feeConvertedAmount = "5.94",
                currentStep = CashWithdrawalStep.FEE_EXCHANGE_RATE
            )
        )
    }
}

// ── Step 7: Review ────────────────────────────────────────────────────────────

@PreviewComplete
@Composable
private fun WizardReviewStepPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "10000",
                showExchangeRateSection = true,
                displayExchangeRate = "37.037",
                deductedAmount = "270.00",
                withdrawalScope = PayerType.GROUP,
                title = "BKK Airport ATM",
                notes = "Kasikorn bank ATM, free withdrawal",
                hasFee = true,
                feeAmount = "220",
                feeCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                showFeeExchangeRateSection = true,
                feeExchangeRate = "37.037",
                feeConvertedAmount = "5.94",
                groupMembers = PREVIEW_MEMBERS,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés",
                currentStep = CashWithdrawalStep.REVIEW
            )
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun WizardReviewStepNarrowPreview() {
    PreviewThemeWrapper {
        AddCashWithdrawalScreen(
            groupId = "group-1",
            uiState = AddCashWithdrawalUiState(
                isConfigLoaded = true,
                groupName = "Thai 2.0",
                groupCurrency = CurrencyUiModel("EUR", "EUR - Euro", 2),
                selectedCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                availableCurrencies = PREVIEW_CURRENCIES,
                withdrawalAmount = "10000",
                showExchangeRateSection = true,
                displayExchangeRate = "37.037",
                deductedAmount = "270.00",
                withdrawalScope = PayerType.GROUP,
                title = "BKK Airport ATM",
                notes = "Kasikorn bank ATM, free withdrawal",
                hasFee = true,
                feeAmount = "220",
                feeCurrency = CurrencyUiModel("THB", "THB - Thai Baht", 2),
                showFeeExchangeRateSection = true,
                feeExchangeRate = "37.037",
                feeConvertedAmount = "5.94",
                groupMembers = PREVIEW_MEMBERS,
                selectedMemberId = "user-1",
                selectedMemberDisplayName = "Andrés",
                currentStep = CashWithdrawalStep.REVIEW
            )
        )
    }
}
