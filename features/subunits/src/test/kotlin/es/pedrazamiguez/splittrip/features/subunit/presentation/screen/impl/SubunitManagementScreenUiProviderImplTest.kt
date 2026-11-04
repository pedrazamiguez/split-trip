package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubunitManagementScreenUiProviderImplTest {

    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase = mockk()
    private val featureGateService: FeatureGateService = mockk()
    private val authenticationService: AuthenticationService = mockk()

    private val uiProvider = SubunitManagementScreenUiProviderImpl(
        observeSelectedGroupUseCase = observeSelectedGroupUseCase,
        featureGateService = featureGateService,
        authenticationService = authenticationService
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
            authenticationService = authenticationService,
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
    fun `handleMainActionClick navigates to account status when creation disabled and user is anonymous`() {
        every { authenticationService.isAnonymous() } returns true
        var navigatedDestination: String? = null
        val navigateToSubscriptions = {
            val destination = if (authenticationService.isAnonymous()) {
                Routes.SETTINGS_ACCOUNT_STATUS
            } else {
                Routes.SETTINGS_SUBSCRIPTIONS
            }
            navigatedDestination = destination
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

        assertEquals(Routes.SETTINGS_ACCOUNT_STATUS, navigatedDestination)
    }

    @Test
    fun `handleMainActionClick navigates to subscriptions when creation disabled and user is registered free-tier`() {
        every { authenticationService.isAnonymous() } returns false
        var navigatedDestination: String? = null
        val navigateToSubscriptions = {
            val destination = if (authenticationService.isAnonymous()) {
                Routes.SETTINGS_ACCOUNT_STATUS
            } else {
                Routes.SETTINGS_SUBSCRIPTIONS
            }
            navigatedDestination = destination
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

        assertEquals(Routes.SETTINGS_SUBSCRIPTIONS, navigatedDestination)
    }
}
