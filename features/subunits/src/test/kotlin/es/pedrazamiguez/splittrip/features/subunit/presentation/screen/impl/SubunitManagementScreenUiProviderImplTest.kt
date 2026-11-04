package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubunitManagementScreenUiProviderImplTest {

    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase = mockk()
    private val featureGateService: FeatureGateService = mockk()
    private val upgradeNavigator: UpgradeNavigator = mockk(relaxed = true)

    private val uiProvider = SubunitManagementScreenUiProviderImpl(
        observeSelectedGroupUseCase = observeSelectedGroupUseCase,
        featureGateService = featureGateService,
        upgradeNavigator = upgradeNavigator
    )

    @Test
    fun `route matches Routes MANAGE_SUBUNITS`() {
        assertEquals(Routes.MANAGE_SUBUNITS, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected custom route`() {
        val customRouteProvider = SubunitManagementScreenUiProviderImpl(
            observeSelectedGroupUseCase = observeSelectedGroupUseCase,
            featureGateService = featureGateService,
            upgradeNavigator = upgradeNavigator,
            route = "custom_route"
        )
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `handleMainActionClick navigates to create when subunit creation is enabled`() {
        var createdRoute: String? = null
        var navigatedToSubscriptions = false
        var shownNotification: String? = null

        uiProvider.handleMainActionClick(
            isSubunitCreationEnabled = true,
            isActingUserPro = false,
            groupId = "group-1",
            proRequiredMessage = "Pro required",
            navigateToCreate = { createdRoute = it },
            navigateToSubscriptions = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertEquals(Routes.createEditSubunitRoute("group-1"), createdRoute)
        assertFalse(navigatedToSubscriptions)
        assertEquals(null, shownNotification)
    }

    @Test
    fun `handleMainActionClick shows notification and navigates to subscriptions when disabled and user is not pro`() {
        var createdRoute: String? = null
        var navigatedToSubscriptions = false
        var shownNotification: String? = null

        uiProvider.handleMainActionClick(
            isSubunitCreationEnabled = false,
            isActingUserPro = false,
            groupId = "group-1",
            proRequiredMessage = "Pro required",
            navigateToCreate = { createdRoute = it },
            navigateToSubscriptions = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertEquals(null, createdRoute)
        assertTrue(navigatedToSubscriptions)
        assertEquals("Pro required", shownNotification)
    }

    @Test
    fun `handleMainActionClick shows notification without navigating to subscriptions when disabled and user is pro`() {
        var createdRoute: String? = null
        var navigatedToSubscriptions = false
        var shownNotification: String? = null

        uiProvider.handleMainActionClick(
            isSubunitCreationEnabled = false,
            isActingUserPro = true,
            groupId = "group-1",
            proRequiredMessage = "Pro required",
            navigateToCreate = { createdRoute = it },
            navigateToSubscriptions = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertEquals(null, createdRoute)
        assertFalse(navigatedToSubscriptions)
        assertEquals("Pro required", shownNotification)
    }

    @Test
    fun `shouldShowProBadge returns true when subunit creation disabled and user is not pro`() {
        assertTrue(uiProvider.shouldShowProBadge(isSubunitCreationEnabled = false, isActingUserPro = false))
    }

    @Test
    fun `shouldShowProBadge returns false when subunit creation is enabled`() {
        assertFalse(uiProvider.shouldShowProBadge(isSubunitCreationEnabled = true, isActingUserPro = false))
        assertFalse(uiProvider.shouldShowProBadge(isSubunitCreationEnabled = true, isActingUserPro = true))
    }

    @Test
    fun `shouldShowProBadge returns false when user is pro even if creation disabled`() {
        assertFalse(uiProvider.shouldShowProBadge(isSubunitCreationEnabled = false, isActingUserPro = true))
    }

    @Test
    fun `handleMainActionClick navigates to upgrade when creation disabled and user is not pro`() {
        val mockNavController = mockk<NavController>(relaxed = true)
        val navigateToSubscriptions = {
            upgradeNavigator.navigateToUpgrade(mockNavController)
        }

        uiProvider.handleMainActionClick(
            isSubunitCreationEnabled = false,
            isActingUserPro = false,
            groupId = "group-1",
            proRequiredMessage = "Pro required",
            navigateToCreate = {},
            navigateToSubscriptions = navigateToSubscriptions,
            showNotification = {}
        )

        verify(exactly = 1) { upgradeNavigator.navigateToUpgrade(mockNavController) }
    }
}
