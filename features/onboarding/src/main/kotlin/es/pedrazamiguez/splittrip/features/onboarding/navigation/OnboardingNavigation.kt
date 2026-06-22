package es.pedrazamiguez.splittrip.features.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.onboarding.presentation.feature.OnboardingFeature
import es.pedrazamiguez.splittrip.features.onboarding.presentation.feature.ReconciliationFeature

fun NavGraphBuilder.onboardingGraph(
    onOnboardingComplete: () -> Unit,
    onReconciliationComplete: () -> Unit
) {
    composable(Routes.ONBOARDING) {
        OnboardingFeature(
            onOnboardingComplete = onOnboardingComplete
        )
    }
    composable(Routes.RECONCILIATION) {
        ReconciliationFeature(
            onReconciliationComplete = onReconciliationComplete
        )
    }
}
