package es.pedrazamiguez.splittrip.core.designsystem.preview.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.ShareLockButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardFastTrackBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardNavigationBarConfig
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardReviewRow
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
                submitLabel = "Create Expense"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {}
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun WizardNavigationBarNarrowPreview() {
    PreviewThemeWrapper {
        WizardNavigationBar(
            config = WizardNavigationBarConfig(
                canGoNext = true,
                isOnLastStep = false,
                isCurrentStepValid = true,
                isLoading = false,
                backLabel = "Atrás",
                nextLabel = "Siguiente",
                submitLabel = "Añadir gasto"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {}
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
                submitLabel = "Create Expense"
            ),
            onBack = {},
            onNext = {},
            onSubmit = {}
        )
    }
}

@PreviewThemes
@Composable
private fun WizardFastTrackBannerStandardPreview() {
    PreviewThemeWrapper {
        WizardFastTrackBanner(
            label = "Skip to Review",
            optionalDefaultsHint = "Defaults: equal split · paid today",
            onSkipToReview = {}
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun WizardFastTrackBannerNarrowPreview() {
    PreviewThemeWrapper {
        WizardFastTrackBanner(
            label = "Ir al resumen",
            optionalDefaultsHint = "Por defecto: reparto igual · pagado hoy",
            onSkipToReview = {}
        )
    }
}

@PreviewThemes
@Composable
private fun WizardFastTrackBannerNoHintPreview() {
    PreviewThemeWrapper {
        WizardFastTrackBanner(
            label = "Skip to Review",
            optionalDefaultsHint = null,
            onSkipToReview = {}
        )
    }
}

@PreviewThemes
@PreviewNarrowThemes
@Composable
private fun WizardReviewRowAndShareLockPreview() {
    PreviewThemeWrapper {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            SectionCard(title = "Review") {
                WizardReviewRow(label = "Expense", value = "Dinner at Sukhumvit")
                WizardReviewRow(label = "Amount", value = "1,450.00 THB")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                ShareLockButton(
                    isLocked = true,
                    contentDescription = "Unlock share",
                    onClick = {}
                )
                ShareLockButton(
                    isLocked = false,
                    contentDescription = "Lock share",
                    onClick = {}
                )
            }
        }
    }
}
