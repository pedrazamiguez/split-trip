package es.pedrazamiguez.splittrip.core.designsystem.preview.wizard

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes

@PreviewThemes
@Composable
private fun WizardNavigationBarStandardPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Back",
                nextLabel = "Next",
                submitLabel = "Create Expense",
                skipToReviewLabel = null,
                optionalDefaultsHint = null
            ),
            onBack = {},
            onNext = {},
            onSubmit = {}
        )
    }
}

@PreviewThemes
@Composable
private fun WizardNavigationBarFastTrackEnglishPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Back",
                nextLabel = "Next",
                submitLabel = "Create Expense",
                skipToReviewLabel = "Skip to Review",
                optionalDefaultsHint = "Defaults: equal split · paid today"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {},
            onSkipToReview = {}
        )
    }
}

@PreviewThemes
@Composable
private fun WizardNavigationBarFastTrackSpanishPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Atrás",
                nextLabel = "Siguiente",
                submitLabel = "Añadir gasto",
                skipToReviewLabel = "Ir al resumen",
                optionalDefaultsHint = "Por defecto: reparto igual · pagado hoy"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {},
            onSkipToReview = {}
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun WizardNavigationBarFastTrackNarrowPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Atrás",
                nextLabel = "Siguiente",
                submitLabel = "Añadir gasto",
                skipToReviewLabel = "Ir al resumen",
                optionalDefaultsHint = "Por defecto: reparto igual · pagado hoy"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {},
            onSkipToReview = {}
        )
    }
}

@PreviewThemes
@Composable
private fun WizardNavigationBarFastTrackNoHintPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Back",
                nextLabel = "Next",
                submitLabel = "Create Expense",
                skipToReviewLabel = "Skip to Review",
                optionalDefaultsHint = null
            ),
            onBack = {},
            onNext = {},
            onSubmit = {},
            onSkipToReview = {}
        )
    }
}

@PreviewThemes
@Composable
private fun WizardNavigationBarLastStepPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = true,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Back",
                nextLabel = "Next",
                submitLabel = "Create Expense",
                skipToReviewLabel = "Skip to Review",
                optionalDefaultsHint = "Defaults: equal split · paid today"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {},
            onSkipToReview = {}
        )
    }
}
