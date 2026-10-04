package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewLocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.impl.SubscriptionsUiMapperImpl
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.SubscriptionsScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.SubscriptionsUiState

@PreviewComplete
@Composable
private fun SubscriptionsScreenPreview() {
    val context = LocalContext.current
    val mapper = SubscriptionsUiMapperImpl(PreviewLocaleProvider(context))
    val plans = mapper.mapPlans(
        currentTier = SubscriptionTier.FREE,
        selectedInterval = BillingInterval.ANNUAL,
        products = emptyList()
    )

    PreviewThemeWrapper {
        SubscriptionsScreen(
            uiState = SubscriptionsUiState(
                selectedInterval = BillingInterval.ANNUAL,
                plans = plans,
                currentTier = SubscriptionTier.FREE,
                isAnonymous = false,
                isLoading = false
            ),
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun SubscriptionsScreenProPlanPreview() {
    val context = LocalContext.current
    val mapper = SubscriptionsUiMapperImpl(PreviewLocaleProvider(context))
    val plans = mapper.mapPlans(
        currentTier = SubscriptionTier.PRO,
        selectedInterval = BillingInterval.MONTHLY,
        products = emptyList()
    )

    PreviewThemeWrapper {
        SubscriptionsScreen(
            uiState = SubscriptionsUiState(
                selectedInterval = BillingInterval.MONTHLY,
                plans = plans,
                currentTier = SubscriptionTier.PRO,
                isAnonymous = false,
                isLoading = false
            ),
            onEvent = {}
        )
    }
}
