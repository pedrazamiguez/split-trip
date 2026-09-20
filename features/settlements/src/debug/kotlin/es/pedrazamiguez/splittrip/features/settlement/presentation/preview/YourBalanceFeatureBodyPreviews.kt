package es.pedrazamiguez.splittrip.features.settlement.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.YourBalanceActivityBreakdown
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.YourBalanceHeroBanner
import es.pedrazamiguez.splittrip.features.settlement.presentation.component.YourBalancePocketCashRow
import es.pedrazamiguez.splittrip.features.settlement.presentation.feature.YourBalanceFeatureBody
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.NetPositionStatus
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.PersonalPositionUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.YourBalanceUiState

private val PREVIEW_POSITION_POSITIVE = PersonalPositionUiModel(
    groupCurrencyCode = "EUR",
    formattedNetPosition = "+45.50 €",
    netPositionStatus = NetPositionStatus.POSITIVE,
    formattedPocketBalance = "35.50 €",
    formattedCashInHand = "10.00 €",
    hasNegativeCashInHand = false,
    formattedTotalContributed = "150.00 €",
    formattedTotalSpent = "104.50 €",
    formattedCashSpent = "40.00 €",
    formattedNonCashSpent = "64.50 €",
    formattedRefundableSpent = "15.00 €",
    formattedTotalFees = "3.50 €"
)

private val PREVIEW_POSITION_NEGATIVE = PersonalPositionUiModel(
    groupCurrencyCode = "EUR",
    formattedNetPosition = "-25.00 €",
    netPositionStatus = NetPositionStatus.NEGATIVE,
    formattedPocketBalance = "-25.00 €",
    formattedCashInHand = "0.00 €",
    hasNegativeCashInHand = false,
    formattedTotalContributed = "50.00 €",
    formattedTotalSpent = "75.00 €",
    formattedCashSpent = "20.00 €",
    formattedNonCashSpent = "55.00 €"
)

@PreviewComplete
@Composable
private fun YourBalanceFeatureBodyPositivePreview() {
    PreviewThemeWrapper {
        YourBalanceFeatureBody(
            uiState = YourBalanceUiState(
                isLoading = false,
                personalPosition = PREVIEW_POSITION_POSITIVE
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun YourBalanceFeatureBodyNegativePreview() {
    PreviewThemeWrapper {
        YourBalanceFeatureBody(
            uiState = YourBalanceUiState(
                isLoading = false,
                personalPosition = PREVIEW_POSITION_NEGATIVE
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun YourBalanceHeroBannerPreview() {
    PreviewThemeWrapper {
        YourBalanceHeroBanner(
            personalPosition = PREVIEW_POSITION_POSITIVE
        )
    }
}

@PreviewComplete
@Composable
private fun YourBalancePocketCashRowPreview() {
    PreviewThemeWrapper {
        YourBalancePocketCashRow(
            personalPosition = PREVIEW_POSITION_POSITIVE,
            onShowCashBreakdown = {}
        )
    }
}

@PreviewComplete
@Composable
private fun YourBalanceActivityBreakdownPreview() {
    PreviewThemeWrapper {
        YourBalanceActivityBreakdown(
            personalPosition = PREVIEW_POSITION_POSITIVE
        )
    }
}
