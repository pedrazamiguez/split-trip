package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardScaffold
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.ReceiptAnalysisOverlay
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import timber.log.Timber

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun ExpenseWizard(
    groupId: String?,
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val titleLabel = stringResource(R.string.expense_wizard_step_title)
    val paymentMethodLabel = stringResource(R.string.expense_wizard_step_payment_method)
    val fundingSourceLabel = stringResource(R.string.expense_wizard_step_funding_source)
    val contributionScopeLabel = stringResource(R.string.expense_wizard_step_contribution_scope)
    val amountLabel = stringResource(R.string.expense_wizard_step_amount)
    val rateLabel = stringResource(R.string.expense_wizard_step_exchange_rate)
    val splitLabel = stringResource(R.string.expense_wizard_step_split)
    val categoryLabel = stringResource(R.string.expense_wizard_step_category)
    val vendorNotesLabel = stringResource(R.string.expense_wizard_step_vendor_notes)
    val paymentStatusLabel = stringResource(R.string.expense_wizard_step_payment_status)
    val receiptLabel = stringResource(R.string.expense_wizard_step_receipt)
    val addOnsLabel = stringResource(R.string.expense_wizard_step_add_ons)
    val reviewLabel = stringResource(R.string.expense_wizard_step_review)

    val stepLabelMap = remember(
        titleLabel, paymentMethodLabel, fundingSourceLabel, contributionScopeLabel,
        amountLabel, rateLabel, splitLabel,
        categoryLabel, vendorNotesLabel, paymentStatusLabel, receiptLabel, addOnsLabel, reviewLabel
    ) {
        mapOf(
            AddExpenseStep.TITLE to titleLabel,
            AddExpenseStep.PAYMENT_METHOD to paymentMethodLabel,
            AddExpenseStep.FUNDING_SOURCE to fundingSourceLabel,
            AddExpenseStep.CONTRIBUTION_SCOPE to contributionScopeLabel,
            AddExpenseStep.AMOUNT to amountLabel,
            AddExpenseStep.EXCHANGE_RATE to rateLabel,
            AddExpenseStep.SPLIT to splitLabel,
            AddExpenseStep.CATEGORY to categoryLabel,
            AddExpenseStep.VENDOR_NOTES to vendorNotesLabel,
            AddExpenseStep.PAYMENT_STATUS to paymentStatusLabel,
            AddExpenseStep.RECEIPT to receiptLabel,
            AddExpenseStep.ADD_ONS to addOnsLabel,
            AddExpenseStep.REVIEW to reviewLabel
        )
    }

    val orderedLabels = remember(uiState.applicableSteps, stepLabelMap) {
        uiState.applicableSteps.map { stepLabelMap[it] ?: "" }
    }

    val backLabel = stringResource(R.string.expense_wizard_back)
    val nextLabel = stringResource(R.string.expense_wizard_next)
    val submitLabel = stringResource(uiState.submitLabelRes)

    LaunchedEffect(uiState.isOnReviewStep, uiState.isFormValid, uiState.isLoading) {
        if (uiState.isOnReviewStep) {
            Timber.d(
                "AddExpenseScreen REVIEW: formValid=%s titleValid=%s amountValid=%s dueValid=%s " +
                    "addOnsValid=%s title='%s' amount='%s' loading=%s",
                uiState.isFormValid,
                uiState.isTitleValid,
                uiState.isAmountValid,
                uiState.isDueDateValid,
                uiState.addOns.all { it.isAmountValid },
                uiState.expenseTitle,
                uiState.sourceAmount,
                uiState.isLoading
            )
        }
    }

    WizardScaffold(
        stepLabels = orderedLabels,
        currentStepIndex = uiState.currentStepIndex,
        optionalStepIndices = uiState.optionalStepIndices,
        allowForwardJumps = uiState.isEditMode,
        onStepClicked = { onEvent(AddExpenseUiEvent.JumpToStep(it)) },
        navigationConfig = WizardNavigationBarConfig(
            canGoNext = uiState.canGoNext,
            isOnLastStep = uiState.isOnReviewStep,
            isCurrentStepValid = uiState.isCurrentStepValid,
            isLoading = uiState.isLoading,
            backLabel = backLabel,
            nextLabel = nextLabel,
            submitLabel = submitLabel
        ),
        onBack = { onEvent(AddExpenseUiEvent.PreviousStep) },
        onNext = { onEvent(AddExpenseUiEvent.NextStep) },
        onSubmit = {
            Timber.d(
                "AddExpenseScreen: submit tapped groupId=%s isEditMode=%s isCurrentStepValid=%s isLoading=%s",
                groupId,
                uiState.isEditMode,
                uiState.isCurrentStepValid,
                uiState.isLoading
            )
            onEvent(AddExpenseUiEvent.SubmitAddExpense(groupId))
        },
        modifier = modifier,
        headerBanner = {
            AnimatedVisibility(
                visible = uiState.autoFillBanner != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                uiState.autoFillBanner?.let { banner ->
                    AutoFillBanner(
                        banner = banner,
                        onDismiss = { onEvent(AddExpenseUiEvent.DismissAutoFillBanner) }
                    )
                }
            }
        },
        overlay = {
            ReceiptAnalysisOverlay(visible = uiState.isAnalyzingReceipt)
        }
    ) {
        WizardStepContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize()
        )
    }
}
