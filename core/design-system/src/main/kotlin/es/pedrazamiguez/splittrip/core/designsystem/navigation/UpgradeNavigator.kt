package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService

/**
 * Handles navigation to the upgrade/paywall destination based on the user's authentication state.
 */
interface UpgradeNavigator {

    /**
     * Returns the target route for paywalls or upgrades:
     * - [Routes.SETTINGS_ACCOUNT_STATUS] if the current user is anonymous (guest).
     * - [Routes.SETTINGS_SUBSCRIPTIONS] if the user is registered.
     */
    fun getUpgradeDestination(): String

    /**
     * Navigates the given [navController] to the appropriate upgrade/paywall destination.
     */
    fun navigateToUpgrade(navController: NavController)
}

class UpgradeNavigatorImpl(
    private val authenticationService: AuthenticationService
) : UpgradeNavigator {

    override fun getUpgradeDestination(): String =
        NavigationUtils.resolveUpgradeDestination(authenticationService.isAnonymous())

    override fun navigateToUpgrade(navController: NavController) {
        navController.navigate(getUpgradeDestination())
    }
}
