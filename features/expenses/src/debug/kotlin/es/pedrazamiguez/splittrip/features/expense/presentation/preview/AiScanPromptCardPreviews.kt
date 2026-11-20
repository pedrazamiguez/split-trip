package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.AiScanPromptCard

@PreviewComplete
@Composable
private fun AiScanPromptCardFreeTrialRemainingPreview() {
    PreviewThemeWrapper {
        AiScanPromptCard(
            isProFeature = false,
            onSwitchToAi = {},
            remainingScansLabel = "3 free scans left this month",
            isQuotaExhausted = false
        )
    }
}

@PreviewComplete
@Composable
private fun AiScanPromptCardFreeTrialExhaustedPreview() {
    PreviewThemeWrapper {
        AiScanPromptCard(
            isProFeature = true,
            onSwitchToAi = {},
            remainingScansLabel = "0 free scans left this month",
            isQuotaExhausted = true
        )
    }
}

@PreviewComplete
@Composable
private fun AiScanPromptCardProPreview() {
    PreviewThemeWrapper {
        AiScanPromptCard(
            isProFeature = false,
            onSwitchToAi = {},
            remainingScansLabel = null,
            isQuotaExhausted = false
        )
    }
}
