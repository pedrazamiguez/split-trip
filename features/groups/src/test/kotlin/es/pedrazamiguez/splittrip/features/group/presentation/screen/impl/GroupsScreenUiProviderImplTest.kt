package es.pedrazamiguez.splittrip.features.group.presentation.screen.impl

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.features.group.R
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GroupsScreenUiProviderImplTest {

    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase = mockk()
    private val authenticationService: AuthenticationService = mockk()
    private val featureGateService: FeatureGateService = mockk()

    private val uiProvider = GroupsScreenUiProviderImpl(
        getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
        authenticationService = authenticationService,
        featureGateService = featureGateService
    )

    private val testMessage = UiText.StringResource(R.string.group_error_limit_groups_exceeded)

    @Test
    fun `route matches Routes GROUPS`() {
        assertEquals(Routes.GROUPS, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected custom route`() {
        val customRouteProvider = GroupsScreenUiProviderImpl(
            getUserGroupsFlowUseCase = getUserGroupsFlowUseCase,
            authenticationService = authenticationService,
            featureGateService = featureGateService,
            route = "custom_route"
        )
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `handleMainActionClick navigates to create group when allowed`() {
        var navigatedToCreate = false
        var navigatedToSubscriptions = false
        var shownNotification: UiText? = null

        uiProvider.handleMainActionClick(
            isAllowed = true,
            isActingUserPro = false,
            limitReachedMessage = testMessage,
            navigateToCreateGroup = { navigatedToCreate = true },
            navigateToSubscriptions = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertTrue(navigatedToCreate)
        assertFalse(navigatedToSubscriptions)
        assertEquals(null, shownNotification)
    }

    @Test
    fun `handleMainActionClick navigates to subscriptions when blocked for non-pro user`() {
        var navigatedToCreate = false
        var navigatedToSubscriptions = false
        var shownNotification: UiText? = null

        uiProvider.handleMainActionClick(
            isAllowed = false,
            isActingUserPro = false,
            limitReachedMessage = testMessage,
            navigateToCreateGroup = { navigatedToCreate = true },
            navigateToSubscriptions = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertFalse(navigatedToCreate)
        assertTrue(navigatedToSubscriptions)
        assertEquals(testMessage, shownNotification)
    }

    @Test
    fun `handleMainActionClick shows notification without paywall when blocked for pro user`() {
        var navigatedToCreate = false
        var navigatedToSubscriptions = false
        var shownNotification: UiText? = null

        uiProvider.handleMainActionClick(
            isAllowed = false,
            isActingUserPro = true,
            limitReachedMessage = testMessage,
            navigateToCreateGroup = { navigatedToCreate = true },
            navigateToSubscriptions = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertFalse(navigatedToCreate)
        assertFalse(navigatedToSubscriptions)
        assertEquals(testMessage, shownNotification)
    }

    @Test
    fun `shouldShowProBadge returns true when not allowed and user is not pro`() {
        assertTrue(uiProvider.shouldShowProBadge(isAllowed = false, isActingUserPro = false))
    }

    @Test
    fun `shouldShowProBadge returns false when allowed`() {
        assertFalse(uiProvider.shouldShowProBadge(isAllowed = true, isActingUserPro = false))
        assertFalse(uiProvider.shouldShowProBadge(isAllowed = true, isActingUserPro = true))
    }

    @Test
    fun `shouldShowProBadge returns false when user is pro even if not allowed`() {
        assertFalse(uiProvider.shouldShowProBadge(isAllowed = false, isActingUserPro = true))
    }
}
