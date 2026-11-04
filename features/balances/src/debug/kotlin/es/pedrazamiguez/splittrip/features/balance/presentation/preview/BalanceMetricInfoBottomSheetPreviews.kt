package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.features.balance.presentation.component.BalanceMetricInfoBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricScopeBreakdownUiModel
import kotlinx.collections.immutable.persistentListOf

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

@PreviewComplete
@Composable
private fun BalanceMetricInfoBottomSheetExtrasPreview() {
    PreviewThemeWrapper {
        BalanceMetricInfoBottomSheet(
            metricType = BalanceMetricType.EXTRAS,
            breakdown = persistentListOf(
                MetricScopeBreakdownUiModel(
                    scopeLabel = "Group",
                    scopeType = PayerType.GROUP,
                    formattedSubtotal = "3,50 €",
                    items = persistentListOf(
                        MetricBreakdownItemUiModel(
                            title = "Fees",
                            formattedAmount = "1,50 €"
                        ),
                        MetricBreakdownItemUiModel(
                            title = "Tips",
                            formattedAmount = "2,00 €"
                        )
                    )
                ),
                MetricScopeBreakdownUiModel(
                    scopeLabel = "You",
                    scopeType = PayerType.USER,
                    formattedSubtotal = "1,20 €",
                    items = persistentListOf(
                        MetricBreakdownItemUiModel(
                            title = "Surcharges",
                            formattedAmount = "1,20 €"
                        )
                    )
                )
            ),
            formattedGrandTotal = "4,70 €",
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun BalanceMetricInfoBottomSheetScheduledPreview() {
    PreviewThemeWrapper {
        BalanceMetricInfoBottomSheet(
            metricType = BalanceMetricType.SCHEDULED,
            breakdown = persistentListOf(
                MetricScopeBreakdownUiModel(
                    scopeLabel = "Group",
                    scopeType = PayerType.GROUP,
                    formattedSubtotal = "150,00 €",
                    items = persistentListOf(
                        MetricBreakdownItemUiModel(
                            title = "Hotel reservation",
                            subtitle = "15 Jul",
                            formattedAmount = "150,00 €"
                        )
                    )
                )
            ),
            formattedGrandTotal = "150,00 €",
            onDismiss = {}
        )
    }
}
