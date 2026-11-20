package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UpgradeNavigatorImpl")
class UpgradeNavigatorImplTest {

    private lateinit var authenticationService: AuthenticationService
    private lateinit var navController: NavController
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var upgradeNavigator: UpgradeNavigatorImpl

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        navController = mockk(relaxed = true)
        telemetryTracker = mockk(relaxed = true)
        upgradeNavigator = UpgradeNavigatorImpl(authenticationService, telemetryTracker)
    }

    @Test
    fun `getUpgradeDestination returns SETTINGS_ACCOUNT_STATUS when user is anonymous`() {
        every { authenticationService.isAnonymous() } returns true

        val destination = upgradeNavigator.getUpgradeDestination()

        assertEquals(Routes.SETTINGS_ACCOUNT_STATUS, destination)
    }

    @Test
    fun `getUpgradeDestination returns SETTINGS_SUBSCRIPTIONS when user is registered`() {
        every { authenticationService.isAnonymous() } returns false

        val destination = upgradeNavigator.getUpgradeDestination()

        assertEquals(Routes.SETTINGS_SUBSCRIPTIONS, destination)
    }

    @Test
    fun `navigateToUpgrade navigates to SETTINGS_ACCOUNT_STATUS when user is anonymous`() {
        every { authenticationService.isAnonymous() } returns true

        upgradeNavigator.navigateToUpgrade(navController)

        verify(exactly = 1) { navController.navigate(Routes.SETTINGS_ACCOUNT_STATUS) }
    }

    @Test
    fun `navigateToUpgrade navigates to SETTINGS_SUBSCRIPTIONS when user is registered`() {
        every { authenticationService.isAnonymous() } returns false

        upgradeNavigator.navigateToUpgrade(navController)

        verify(exactly = 1) { navController.navigate(Routes.SETTINGS_SUBSCRIPTIONS) }
    }

    @Test
    fun `navigateToUpgrade tracks upgrade_routed with subscriptions when registered and source provided`() {
        every { authenticationService.isAnonymous() } returns false

        upgradeNavigator.navigateToUpgrade(navController, source = MonetizationTelemetry.EntrySource.GROUPS_FAB)

        verify(exactly = 1) {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.UPGRADE_ROUTED,
                mapOf(
                    MonetizationTelemetry.Param.ENTRY_SOURCE to MonetizationTelemetry.EntrySource.GROUPS_FAB,
                    MonetizationTelemetry.Param.DESTINATION to MonetizationTelemetry.Destination.SUBSCRIPTIONS,
                    MonetizationTelemetry.Param.IS_ANONYMOUS to false
                )
            )
            navController.navigate(Routes.settingsSubscriptionsRoute(MonetizationTelemetry.EntrySource.GROUPS_FAB))
        }
    }

    @Test
    fun `navigateToUpgrade tracks upgrade_routed with account_status when anonymous and source is null`() {
        every { authenticationService.isAnonymous() } returns true

        upgradeNavigator.navigateToUpgrade(navController, source = null)

        verify(exactly = 1) {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.UPGRADE_ROUTED,
                mapOf(
                    MonetizationTelemetry.Param.ENTRY_SOURCE to MonetizationTelemetry.EntrySource.DIRECT,
                    MonetizationTelemetry.Param.DESTINATION to MonetizationTelemetry.Destination.ACCOUNT_STATUS,
                    MonetizationTelemetry.Param.IS_ANONYMOUS to true
                )
            )
            navController.navigate(Routes.SETTINGS_ACCOUNT_STATUS)
        }
    }
}
