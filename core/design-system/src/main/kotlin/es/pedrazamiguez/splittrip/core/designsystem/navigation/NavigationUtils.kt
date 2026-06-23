package es.pedrazamiguez.splittrip.core.designsystem.navigation

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
     * Filters and sorts [NavigationProvider]s to only include tabs that should
     * be visible given the current group-selection state.
     *
     * Providers with [NavigationProvider.requiresSelectedGroup] == `true` are
     * hidden when no group is selected (`selectedGroupId == null`).
     *
     * @param providers All registered navigation providers.
     * @param selectedGroupId The currently selected group ID, or `null`.
     * @return Visible providers sorted by [NavigationProvider.order].
     */
    fun filterVisibleProviders(
        providers: List<NavigationProvider>,
        selectedGroupId: String?
    ): List<NavigationProvider> = providers
        .filter { !it.requiresSelectedGroup || selectedGroupId != null }
        .sortedBy { it.order }
}
