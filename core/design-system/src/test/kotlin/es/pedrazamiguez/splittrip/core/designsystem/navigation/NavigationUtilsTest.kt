package es.pedrazamiguez.splittrip.core.designsystem.navigation

import es.pedrazamiguez.splittrip.domain.model.Group
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("NavigationUtils")
class NavigationUtilsTest {

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun fakeProvider(route: String, order: Int, requiresSelectedGroup: Boolean = false): NavigationProvider =
        mockk<NavigationProvider>(relaxed = true).also {
            every { it.route } returns route
            every { it.order } returns order
            every { it.requiresSelectedGroup } returns requiresSelectedGroup
        }

    // ═════════════════════════════════════════════════════════════════════
    //  resolveStartDestination
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveStartDestination")
    inner class ResolveStartDestination {

        @Test
        fun `returns null when auth state is unknown`() {
            assertNull(
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = null,
                    onboardingCompleted = true,
                    isReconciled = true,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }

        @Test
        fun `returns null when onboarding state is unknown`() {
            assertNull(
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = null,
                    isReconciled = true,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }

        @Test
        fun `returns null when both states are unknown`() {
            assertNull(
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = null,
                    onboardingCompleted = null,
                    isReconciled = true,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }

        @Test
        fun `returns LOGIN when user is not logged in`() {
            assertEquals(
                Routes.LOGIN,
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = false,
                    onboardingCompleted = false,
                    isReconciled = false,
                    isReconciliationChecked = false,
                    hasPendingReconciliation = null
                )
            )
        }

        @Test
        fun `returns LOGIN when user is not logged in regardless of onboarding`() {
            assertEquals(
                Routes.LOGIN,
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = false,
                    onboardingCompleted = true,
                    isReconciled = true,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }

        @Test
        fun `returns null when isReconciled is unknown`() {
            assertNull(
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = false,
                    isReconciled = null,
                    isReconciliationChecked = false,
                    hasPendingReconciliation = null
                )
            )
        }

        @Test
        fun `returns null when isReconciled is false and reconciliation is not checked`() {
            assertNull(
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = false,
                    isReconciled = false,
                    isReconciliationChecked = false,
                    hasPendingReconciliation = null
                )
            )
        }

        @Test
        fun `returns RECONCILIATION when isReconciled is false, checked, and has pending data`() {
            assertEquals(
                Routes.RECONCILIATION,
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = false,
                    isReconciled = false,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = true
                )
            )
        }

        @Test
        fun `returns null when isReconciled is false, checked, but does not have pending data`() {
            assertNull(
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = false,
                    isReconciled = false,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }

        @Test
        fun `returns ONBOARDING when logged in but onboarding not completed`() {
            assertEquals(
                Routes.ONBOARDING,
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = false,
                    isReconciled = true,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }

        @Test
        fun `returns MAIN when logged in and onboarding completed`() {
            assertEquals(
                Routes.MAIN,
                NavigationUtils.resolveStartDestination(
                    isUserLoggedIn = true,
                    onboardingCompleted = true,
                    isReconciled = true,
                    isReconciliationChecked = true,
                    hasPendingReconciliation = false
                )
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  resolvePostLoginDestination
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolvePostLoginDestination")
    inner class ResolvePostLoginDestination {

        @Test
        fun `returns MAIN when onboarding is completed`() {
            assertEquals(
                Routes.MAIN,
                NavigationUtils.resolvePostLoginDestination(onboardingCompleted = true)
            )
        }

        @Test
        fun `returns ONBOARDING when onboarding is not completed`() {
            assertEquals(
                Routes.ONBOARDING,
                NavigationUtils.resolvePostLoginDestination(onboardingCompleted = false)
            )
        }

        @Test
        fun `returns ONBOARDING when onboarding state is null`() {
            assertEquals(
                Routes.ONBOARDING,
                NavigationUtils.resolvePostLoginDestination(onboardingCompleted = null)
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  resolveUpgradeDestination
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resolveUpgradeDestination")
    inner class ResolveUpgradeDestination {

        @Test
        fun `returns SETTINGS_ACCOUNT_STATUS when user is anonymous`() {
            assertEquals(
                Routes.SETTINGS_ACCOUNT_STATUS,
                NavigationUtils.resolveUpgradeDestination(isAnonymous = true)
            )
        }

        @Test
        fun `returns SETTINGS_SUBSCRIPTIONS when user is not anonymous`() {
            assertEquals(
                Routes.SETTINGS_SUBSCRIPTIONS,
                NavigationUtils.resolveUpgradeDestination(isAnonymous = false)
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  filterVisibleProviders
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("filterVisibleProviders")
    inner class FilterVisibleProviders {

        private val groups = fakeProvider(route = Routes.GROUPS, order = 10)
        private val expenses = fakeProvider(
            route = Routes.EXPENSES,
            order = 50,
            requiresSelectedGroup = true
        )
        private val balances = fakeProvider(
            route = Routes.BALANCES,
            order = 20,
            requiresSelectedGroup = true
        )
        private val profile = fakeProvider(route = Routes.PROFILE, order = 90)

        private val allProviders = listOf(groups, expenses, balances, profile)

        private val dummyGroup = Group(
            id = "group-123",
            name = "Test Group",
            currency = "EUR"
        )

        @Test
        fun `shows all providers when a group is selected`() {
            val result = NavigationUtils.filterVisibleProviders(
                providers = allProviders,
                selectedGroup = dummyGroup
            )

            assertEquals(4, result.size)
            assertEquals(
                listOf(Routes.GROUPS, Routes.BALANCES, Routes.EXPENSES, Routes.PROFILE),
                result.map { it.route }
            )
        }

        @Test
        fun `hides group-dependent providers when no group is selected`() {
            val result = NavigationUtils.filterVisibleProviders(
                providers = allProviders,
                selectedGroup = null
            )

            assertEquals(2, result.size)
            assertEquals(
                listOf(Routes.GROUPS, Routes.PROFILE),
                result.map { it.route }
            )
        }

        @Test
        fun `returns providers sorted by order`() {
            // Provide out-of-order list
            val unsorted = listOf(profile, expenses, groups, balances)
            val result = NavigationUtils.filterVisibleProviders(
                providers = unsorted,
                selectedGroup = dummyGroup
            )

            assertEquals(
                listOf(10, 20, 50, 90),
                result.map { it.order }
            )
        }

        @Test
        fun `returns empty list when no providers are given`() {
            val result = NavigationUtils.filterVisibleProviders(
                providers = emptyList(),
                selectedGroup = dummyGroup
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `returns empty list when all providers require a group and none is selected`() {
            val groupOnly = listOf(expenses, balances)
            val result = NavigationUtils.filterVisibleProviders(
                providers = groupOnly,
                selectedGroup = null
            )

            assertTrue(result.isEmpty())
        }

        @Test
        fun `shows all non-group-dependent providers when none require a group`() {
            val noGroupRequired = listOf(groups, profile)
            val result = NavigationUtils.filterVisibleProviders(
                providers = noGroupRequired,
                selectedGroup = null
            )

            assertEquals(2, result.size)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  isAuthRoute
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("isAuthRoute")
    inner class IsAuthRoute {

        @Test
        fun `returns true for LOGIN, REGISTER, and FORGOT_PASSWORD`() {
            assertTrue(NavigationUtils.isAuthRoute(Routes.LOGIN))
            assertTrue(NavigationUtils.isAuthRoute(Routes.REGISTER))
            assertTrue(NavigationUtils.isAuthRoute(Routes.FORGOT_PASSWORD))
        }

        @Test
        fun `returns false for MAIN, ONBOARDING, RECONCILIATION, and null`() {
            assertFalse(NavigationUtils.isAuthRoute(Routes.MAIN))
            assertFalse(NavigationUtils.isAuthRoute(Routes.ONBOARDING))
            assertFalse(NavigationUtils.isAuthRoute(Routes.RECONCILIATION))
            assertFalse(NavigationUtils.isAuthRoute(null))
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  shouldHandleSessionExpired
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("shouldHandleSessionExpired")
    inner class ShouldHandleSessionExpired {

        @Test
        fun `returns true when transitioning true to false and not explicit sign-out`() {
            assertTrue(
                NavigationUtils.shouldHandleSessionExpired(
                    previousLoggedIn = true,
                    currentLoggedIn = false,
                    isExplicitSignOut = false
                )
            )
        }

        @Test
        fun `returns false when transitioning true to false during explicit sign-out`() {
            assertFalse(
                NavigationUtils.shouldHandleSessionExpired(
                    previousLoggedIn = true,
                    currentLoggedIn = false,
                    isExplicitSignOut = true
                )
            )
        }

        @Test
        fun `returns false on initial null to false emission`() {
            assertFalse(
                NavigationUtils.shouldHandleSessionExpired(
                    previousLoggedIn = null,
                    currentLoggedIn = false,
                    isExplicitSignOut = false
                )
            )
        }

        @Test
        fun `returns false when staying true or transitioning false to true`() {
            assertFalse(
                NavigationUtils.shouldHandleSessionExpired(
                    previousLoggedIn = true,
                    currentLoggedIn = true,
                    isExplicitSignOut = false
                )
            )
            assertFalse(
                NavigationUtils.shouldHandleSessionExpired(
                    previousLoggedIn = false,
                    currentLoggedIn = true,
                    isExplicitSignOut = false
                )
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  shouldNavigateOnSessionExpired
    // ═════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("shouldNavigateOnSessionExpired")
    inner class ShouldNavigateOnSessionExpired {

        @Test
        fun `returns true for MAIN, ONBOARDING, RECONCILIATION, and SETTINGS`() {
            assertTrue(NavigationUtils.shouldNavigateOnSessionExpired(Routes.MAIN))
            assertTrue(NavigationUtils.shouldNavigateOnSessionExpired(Routes.ONBOARDING))
            assertTrue(NavigationUtils.shouldNavigateOnSessionExpired(Routes.RECONCILIATION))
            assertTrue(NavigationUtils.shouldNavigateOnSessionExpired(Routes.SETTINGS))
        }

        @Test
        fun `returns false for LOGIN, REGISTER, FORGOT_PASSWORD, and null`() {
            assertFalse(NavigationUtils.shouldNavigateOnSessionExpired(Routes.LOGIN))
            assertFalse(NavigationUtils.shouldNavigateOnSessionExpired(Routes.REGISTER))
            assertFalse(NavigationUtils.shouldNavigateOnSessionExpired(Routes.FORGOT_PASSWORD))
            assertFalse(NavigationUtils.shouldNavigateOnSessionExpired(null))
        }
    }
}
