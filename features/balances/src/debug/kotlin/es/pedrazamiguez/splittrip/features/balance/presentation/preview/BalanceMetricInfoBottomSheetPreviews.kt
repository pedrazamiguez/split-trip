package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalanceMetricInfoBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType

@PreviewComplete
@Composable
private fun BalanceMetricInfoBottomSheetAvailablePreview() {
    PreviewThemeWrapper {
        BalanceMetricInfoBottomSheet(
            metricType = BalanceMetricType.AVAILABLE,
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun BalanceMetricInfoBottomSheetRemainingPreview() {
    PreviewThemeWrapper {
        BalanceMetricInfoBottomSheet(
            metricType = BalanceMetricType.REMAINING,
            onDismiss = {}
        )
    }
}
