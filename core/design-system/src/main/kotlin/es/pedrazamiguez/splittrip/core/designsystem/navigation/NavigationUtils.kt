package es.pedrazamiguez.splittrip.core.designsystem.navigation

import es.pedrazamiguez.splittrip.domain.model.Group

/**
 * Pure utility functions for navigation decision-making.
 *
 * Extracted from composables ([AppNavHost], [MainScreen]) so the critical
 * business logic can be unit-tested without Compose or instrumentation.
 */
object NavigationUtils {

    fun resolveStartDestination(
        isUserLoggedIn: Boolean?,
        onboardingCompleted: Boolean?,
        isReconciled: Boolean?,
        isReconciliationChecked: Boolean,
        hasPendingReconciliation: Boolean?
    ): String? = when {
        isUserLoggedIn == null || onboardingCompleted == null || isReconciled == null -> null
        isUserLoggedIn == false -> Routes.LOGIN
        isReconciled == false -> {
            if (!isReconciliationChecked) {
                null
            } else if (hasPendingReconciliation == true) {
                Routes.RECONCILIATION
            } else {
                null
            }
        }
        onboardingCompleted == false -> Routes.ONBOARDING
        else -> Routes.MAIN
    }

    /**
     * Resolves the destination to navigate to after a successful login.
     *
     * @param onboardingCompleted Whether the user has completed onboarding.
     * @return [Routes.MAIN] if onboarding is done, [Routes.ONBOARDING] otherwise.
     */
    fun resolvePostLoginDestination(onboardingCompleted: Boolean?): String =
        if (onboardingCompleted == true) Routes.MAIN else Routes.ONBOARDING

    /**
     * Resolves the navigation destination when a user encounters a paywall or initiates an upgrade.
     *
     * Anonymous guest users must first link or register an account before purchasing subscriptions,
     * so they are directed to [Routes.SETTINGS_ACCOUNT_STATUS]. Authenticated users are directed
     * to [Routes.SETTINGS_SUBSCRIPTIONS].
     *
     * @param isAnonymous Whether the acting user has an anonymous (guest) auth session.
     * @return [Routes.SETTINGS_ACCOUNT_STATUS] if anonymous, [Routes.SETTINGS_SUBSCRIPTIONS] otherwise.
     */
    fun resolveUpgradeDestination(isAnonymous: Boolean): String =
        if (isAnonymous) Routes.SETTINGS_ACCOUNT_STATUS else Routes.SETTINGS_SUBSCRIPTIONS

    /**
     * Filters and sorts [NavigationProvider]s to only include tabs that should
     * be visible given the current group-selection state.
     *
     * Providers with [NavigationProvider.requiresSelectedGroup] == `true` are
     * hidden when no group is selected (`selectedGroupId == null`).
     *
     * @param providers All registered navigation providers.
     * @param selectedGroup The currently selected group domain object, or `null`.
     * @return Visible providers sorted by [NavigationProvider.order].
     */
    fun filterVisibleProviders(
        providers: List<NavigationProvider>,
        selectedGroup: Group?
    ): List<NavigationProvider> = providers
        .filter { !it.requiresSelectedGroup || selectedGroup != null }
        .sortedBy { it.order }

    /**
     * Returns `true` if [route] belongs to the pre-authentication flow
     * ([Routes.LOGIN], [Routes.REGISTER], or [Routes.FORGOT_PASSWORD]).
     */
    fun isAuthRoute(route: String?): Boolean =
        route == Routes.LOGIN ||
            route == Routes.REGISTER ||
            route == Routes.FORGOT_PASSWORD

    /**
     * Determines whether a session-expiration teardown and feedback pill should run
     * for an auth-state transition.
     *
     * Only triggers on a `true -> false` transition that was NOT initiated by an explicit
     * user sign-out.
     */
    fun shouldHandleSessionExpired(
        previousLoggedIn: Boolean?,
        currentLoggedIn: Boolean?,
        isExplicitSignOut: Boolean
    ): Boolean = previousLoggedIn == true && currentLoggedIn == false && !isExplicitSignOut

    /**
     * Determines whether the root [NavHostController] should redirect to [Routes.LOGIN]
     * following session expiration given the active [currentRoute].
     */
    fun shouldNavigateOnSessionExpired(currentRoute: String?): Boolean =
        currentRoute != null && !isAuthRoute(currentRoute)
}
