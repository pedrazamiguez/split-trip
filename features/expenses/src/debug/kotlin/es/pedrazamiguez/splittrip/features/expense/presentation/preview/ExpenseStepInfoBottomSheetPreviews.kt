package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.component.ExpenseStepInfoBottomSheet
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetTitlePreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.TITLE,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetPaymentMethodPreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.PAYMENT_METHOD,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetExchangeRatePreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.EXCHANGE_RATE,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetSplitPreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.SPLIT,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetFundingSourcePreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.FUNDING_SOURCE,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetContributionScopePreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.CONTRIBUTION_SCOPE,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetPaymentStatusPreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.PAYMENT_STATUS,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetAddOnsPreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.ADD_ONS,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ExpenseStepInfoBottomSheetReviewPreview() {
    PreviewThemeWrapper {
        ExpenseStepInfoBottomSheet(
            step = AddExpenseStep.REVIEW,
            onDismiss = {}
        )
    }
}
