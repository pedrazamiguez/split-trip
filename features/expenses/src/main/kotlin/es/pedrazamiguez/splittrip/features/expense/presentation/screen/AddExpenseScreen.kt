package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Refresh
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepIndicator
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.AddOnsStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.AmountStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.CategoryStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ContributionScopeStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ExchangeRateStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.FundingSourceStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.PaymentMethodStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.PaymentStatusStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ReceiptStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.ReviewStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.SplitStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.TitleStep
import es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense.VendorNotesStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Shared element transition key for the Add Expense FAB -> Screen transition.
 */
const val ADD_EXPENSE_SHARED_ELEMENT_KEY = "add_expense_container"

@Composable
fun AddExpenseScreen(
    groupId: String? = null,
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit = {}
) {
    LaunchedEffect(groupId) {
        onEvent(AddExpenseUiEvent.LoadGroupConfig(groupId))
    }

    SharedTransitionSurface(sharedElementKey = ADD_EXPENSE_SHARED_ELEMENT_KEY) {
        when {
            uiState.isReady -> {
                ExpenseWizard(groupId = groupId, uiState = uiState, onEvent = onEvent)
            }

            uiState.configLoadFailed -> {
                AddExpenseConfigFailedContent(
                    onRetry = { onEvent(AddExpenseUiEvent.RetryLoadConfig(groupId)) }
                )
            }

            else -> {
                ShimmerLoadingList()
            }
        }
    }
}

@Composable
private fun ExpenseWizard(
    groupId: String?,
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val stepLabelMap = rememberStepLabelMap()
    val orderedLabels = remember(uiState.applicableSteps, stepLabelMap) {
        uiState.applicableSteps.map { stepLabelMap[it] ?: "" }
    }

    val backLabel = stringResource(R.string.expense_wizard_back)
    val nextLabel = stringResource(R.string.expense_wizard_next)
    val submitLabel = stringResource(R.string.add_expense_submit_button)
    val skipToReviewLabel = stringResource(R.string.expense_wizard_skip_to_review)

    val bottomPadding = LocalBottomPadding.current

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        ExpenseWizardBody(
            uiState = uiState,
            orderedLabels = orderedLabels,
            skipToReviewLabel = skipToReviewLabel,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize()
        )

        WizardNavigationBar(
            config = WizardNavigationBarConfig(
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
            onSubmit = { onEvent(AddExpenseUiEvent.SubmitAddExpense(groupId)) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
        )
    }
}

@Composable
private fun ExpenseWizardBody(
    uiState: AddExpenseUiState,
    orderedLabels: List<String>,
    skipToReviewLabel: String,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        WizardStepIndicator(
            stepLabels = orderedLabels,
            currentStepIndex = uiState.currentStepIndex,
            optionalStepIndices = uiState.optionalStepIndices,
            skipToReviewLabel = if (uiState.canSkipToReview) skipToReviewLabel else null,
            onSkipToReview = if (uiState.canSkipToReview) {
                { onEvent(AddExpenseUiEvent.JumpToReview) }
            } else {
                null
            },
            onStepClicked = { onEvent(AddExpenseUiEvent.JumpToStep(it)) }
        )

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

        WizardStepContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = Modifier.weight(1f)
        )
    }
}

// Thin step-routing `when` — complexity is proportional to the number of wizard steps
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
private fun WizardStepContent(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = uiState.currentStep,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn())
                .togetherWith(
                    slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut()
                )
                .using(SizeTransform(clip = false))
        },
        label = "wizardStep"
    ) { step ->
        val bottomPadding = LocalBottomPadding.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomPadding + UiConstants.WIZARD_NAV_BAR_HEIGHT)
        ) {
            val nextStep = { onEvent(AddExpenseUiEvent.NextStep) }
            when (step) {
                AddExpenseStep.TITLE -> TitleStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.PAYMENT_METHOD -> PaymentMethodStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.FUNDING_SOURCE -> FundingSourceStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.CONTRIBUTION_SCOPE -> ContributionScopeStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.AMOUNT -> AmountStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.EXCHANGE_RATE -> ExchangeRateStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.SPLIT -> SplitStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.CATEGORY -> CategoryStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onAutoAdvance = nextStep
                )
                AddExpenseStep.VENDOR_NOTES -> VendorNotesStep(
                    uiState = uiState,
                    onEvent = onEvent,
                    onImeNext = nextStep
                )
                AddExpenseStep.PAYMENT_STATUS -> PaymentStatusStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.RECEIPT -> ReceiptStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.ADD_ONS -> AddOnsStep(uiState = uiState, onEvent = onEvent)
                AddExpenseStep.REVIEW -> ReviewStep(uiState = uiState)
            }
        }
    }
}

@Composable
private fun AddExpenseConfigFailedContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = TablerIcons.Outline.Refresh,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Default))
        Text(
            text = stringResource(R.string.expense_error_load_group_config),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
        BodyText(
            text = stringResource(R.string.expense_error_config_retry_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
        SecondaryButton(
            text = stringResource(R.string.expense_error_retry_button),
            onClick = onRetry,
            leadingIcon = TablerIcons.Outline.Refresh
        )
    }
}

/**
 * Creates a remembered map of [AddExpenseStep] → localised label.
 *
 * Using a map (not a list) keeps the table readable and ensures that
 * [ExpenseWizard] can derive the correctly-ordered list from the dynamic
 * [AddExpenseUiState.applicableSteps] at composition time.
 */
@Composable
private fun rememberStepLabelMap(): Map<AddExpenseStep, String> {
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

    return remember(
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
}

@Composable
private fun AutoFillBanner(
    banner: es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AutoFillBanner,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fieldsString = remember(banner.fields, context) {
        banner.fields.map { it.asString(context) }.joinToString(", ")
    }

    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.Medium, vertical = MaterialTheme.spacing.Small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(MaterialTheme.spacing.Medium)
        ) {
            Icon(
                imageVector = TablerIcons.Outline.Receipt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(MaterialTheme.spacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.expense_autofill_banner_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = fieldsString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(
                onClick = onDismiss
            ) {
                Icon(
                    imageVector = TablerIcons.Outline.X,
                    contentDescription = stringResource(R.string.receipt_viewer_close_cd),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
