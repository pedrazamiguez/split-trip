package es.pedrazamiguez.splittrip.core.designsystem.navigation

import androidx.navigation.NavController
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
    private lateinit var upgradeNavigator: UpgradeNavigatorImpl

    @BeforeEach
    fun setUp() {
        authenticationService = mockk()
        navController = mockk(relaxed = true)
        upgradeNavigator = UpgradeNavigatorImpl(authenticationService)
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
}
