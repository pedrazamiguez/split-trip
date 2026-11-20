package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService

/**
 * Handles navigation to the upgrade/paywall destination based on the user's authentication state.
 */
interface UpgradeNavigator {

    /**
     * Returns the target route for paywalls or upgrades:
     * - [Routes.SETTINGS_ACCOUNT_STATUS] if the current user is anonymous (guest).
     * - [Routes.settingsSubscriptionsRoute] if the user is registered.
     */
    fun getUpgradeDestination(source: String? = null): String

    /**
     * Navigates the given [navController] to the appropriate upgrade/paywall destination.
     */
    fun navigateToUpgrade(navController: NavController, source: String? = null)
}

class UpgradeNavigatorImpl(
    private val authenticationService: AuthenticationService,
    private val telemetryTracker: TelemetryTracker
) : UpgradeNavigator {

    override fun getUpgradeDestination(source: String?): String =
        NavigationUtils.resolveUpgradeDestination(
            isAnonymous = authenticationService.isAnonymous(),
            source = source
        )

    override fun navigateToUpgrade(navController: NavController, source: String?) {
        val isAnonymous = authenticationService.isAnonymous()
        val destination = NavigationUtils.resolveUpgradeDestination(
            isAnonymous = isAnonymous,
            source = source
        )
        telemetryTracker.trackEvent(
            MonetizationTelemetry.Event.UPGRADE_ROUTED,
            mapOf(
                MonetizationTelemetry.Param.ENTRY_SOURCE to (source ?: MonetizationTelemetry.EntrySource.DIRECT),
                MonetizationTelemetry.Param.DESTINATION to if (isAnonymous) {
                    MonetizationTelemetry.Destination.ACCOUNT_STATUS
                } else {
                    MonetizationTelemetry.Destination.SUBSCRIPTIONS
                },
                MonetizationTelemetry.Param.IS_ANONYMOUS to isAnonymous
            )
        )
        navController.navigate(destination)
    }
}
