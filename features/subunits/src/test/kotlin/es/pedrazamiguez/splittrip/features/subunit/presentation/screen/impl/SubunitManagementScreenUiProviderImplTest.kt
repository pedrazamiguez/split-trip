package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubunitManagementScreenUiProviderImplTest {

    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase = mockk()
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase = mockk()
    private val featureGateService: FeatureGateService = mockk()
    private val upgradeNavigator: UpgradeNavigator = mockk(relaxed = true)
    private val telemetryTracker: TelemetryTracker = mockk(relaxed = true)

    private val uiProvider = SubunitManagementScreenUiProviderImpl(
        observeSelectedGroupUseCase = observeSelectedGroupUseCase,
        getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
        featureGateService = featureGateService,
        upgradeNavigator = upgradeNavigator,
        telemetryTracker = telemetryTracker
    )

    @Test
    fun `route matches Routes MANAGE_SUBUNITS`() {
        assertEquals(Routes.MANAGE_SUBUNITS, uiProvider.route)
    }

    @Test
    fun `provider configuration has expected custom route`() {
        val customRouteProvider = SubunitManagementScreenUiProviderImpl(
            observeSelectedGroupUseCase = observeSelectedGroupUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            featureGateService = featureGateService,
            upgradeNavigator = upgradeNavigator,
            telemetryTracker = telemetryTracker,
            route = "custom_route"
        )
        assertEquals("custom_route", customRouteProvider.route)
    }

    @Test
    fun `handleMainActionClick navigates to create when limitResult is Allowed`() {
        var createdRoute: String? = null
        var navigatedToSubscriptions = false
        var shownNotification: String? = null

        uiProvider.handleMainActionClick(
            limitResult = LimitResult.Allowed,
            groupId = "group-1",
            limitReachedMessage = "Limit reached",
            navigateToCreate = { createdRoute = it },
            showProPreview = { navigatedToSubscriptions = true },
            showNotification = { shownNotification = it }
        )

        assertEquals(Routes.createEditSubunitRoute("group-1"), createdRoute)
        assertFalse(navigatedToSubscriptions)
        assertEquals(null, shownNotification)
    }

    @Test
    fun `handleMainActionClick opens pro preview without notification when blocked with upgradeRequired true`() {
        var createdRoute: String? = null
        var proPreviewShown = false
        var shownNotification: String? = null

        uiProvider.handleMainActionClick(
            limitResult = LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = true),
            groupId = "group-1",
            limitReachedMessage = "Limit reached",
            navigateToCreate = { createdRoute = it },
            showProPreview = { proPreviewShown = true },
            showNotification = { shownNotification = it }
        )

        assertEquals(null, createdRoute)
        assertTrue(proPreviewShown)
        assertEquals(null, shownNotification)
        verify(exactly = 1) {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                    MonetizationTelemetry.Param.GATE_NAME to "max_subunits_per_group",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.SUBUNIT_FAB,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to true
                )
            )
        }
    }

    @Test
    fun `handleMainActionClick shows notification without pro preview when blocked with upgradeRequired false`() {
        var createdRoute: String? = null
        var proPreviewShown = false
        var shownNotification: String? = null

        uiProvider.handleMainActionClick(
            limitResult = LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = false),
            groupId = "group-1",
            limitReachedMessage = "Limit reached",
            navigateToCreate = { createdRoute = it },
            showProPreview = { proPreviewShown = true },
            showNotification = { shownNotification = it }
        )

        assertEquals(null, createdRoute)
        assertFalse(proPreviewShown)
        assertEquals("Limit reached", shownNotification)
        verify(exactly = 1) {
            telemetryTracker.trackEvent(
                MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                mapOf(
                    MonetizationTelemetry.Param.GATE_TYPE to MonetizationTelemetry.GateType.LIMIT,
                    MonetizationTelemetry.Param.GATE_NAME to "max_subunits_per_group",
                    MonetizationTelemetry.Param.ENTRY_POINT to MonetizationTelemetry.GateEntryPoint.SUBUNIT_FAB,
                    MonetizationTelemetry.Param.UPGRADE_PROMPTED to false
                )
            )
        }
    }

    @Test
    fun `shouldShowProBadge returns true when blocked with upgradeRequired true`() {
        assertTrue(
            uiProvider.shouldShowProBadge(
                LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = true)
            )
        )
    }

    @Test
    fun `shouldShowProBadge returns false when limitResult is Allowed`() {
        assertFalse(uiProvider.shouldShowProBadge(LimitResult.Allowed))
    }

    @Test
    fun `shouldShowProBadge returns false when blocked with upgradeRequired false`() {
        assertFalse(
            uiProvider.shouldShowProBadge(
                LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = false)
            )
        )
    }

    @Test
    fun `handleMainActionClick invokes showProPreview when blocked with upgradeRequired true`() {
        val mockNavController = mockk<NavController>(relaxed = true)
        val showProPreview = {
            upgradeNavigator.navigateToUpgrade(mockNavController)
        }

        uiProvider.handleMainActionClick(
            limitResult = LimitResult.Blocked(GatedLimit.MAX_SUBUNITS_PER_GROUP, upgradeRequired = true),
            groupId = "group-1",
            limitReachedMessage = "Limit reached",
            navigateToCreate = {},
            showProPreview = showProPreview,
            showNotification = {}
        )

        verify(exactly = 1) { upgradeNavigator.navigateToUpgrade(mockNavController) }
    }

    @Test
    fun `shouldShowMainAction returns true when group status is ACTIVE`() {
        assertTrue(uiProvider.shouldShowMainAction(GroupStatus.ACTIVE))
    }

    @Test
    fun `shouldShowMainAction returns true when group status is null`() {
        assertTrue(uiProvider.shouldShowMainAction(null))
    }

    @Test
    fun `shouldShowMainAction returns false when group status is ARCHIVED`() {
        assertFalse(uiProvider.shouldShowMainAction(GroupStatus.ARCHIVED))
    }
}
