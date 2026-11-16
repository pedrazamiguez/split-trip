package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.R

@PreviewComplete
@Composable
private fun ExpenseWizardNavigationBarFastTrackPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = stringResource(R.string.expense_wizard_back),
                nextLabel = stringResource(R.string.expense_wizard_next),
                submitLabel = stringResource(R.string.add_expense_submit_button),
                skipToReviewLabel = stringResource(R.string.expense_wizard_skip_to_review),
                optionalDefaultsHint = stringResource(R.string.expense_wizard_skip_defaults_hint)
            ),
            onBack = {},
            onNext = {},
            onSubmit = {},
            onSkipToReview = {}
        )
    }
}
