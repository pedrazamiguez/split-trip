package es.pedrazamiguez.splittrip.features.onboarding.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.OnboardingScreen
import org.koin.compose.getKoin

@Composable
fun OnboardingFeature(onOnboardingComplete: () -> Unit = {}) {
    val koin = getKoin()
    val telemetryTracker = remember(koin) { koin.get<TelemetryTracker>() }

    LaunchedEffect(Unit) {
        telemetryTracker.trackScreenView("Onboarding", "OnboardingFeature")
    }

    OnboardingScreen(
        onOnboardingComplete = {
            telemetryTracker.trackEvent("onboarding_complete")
            onOnboardingComplete()
        }
    )
}
