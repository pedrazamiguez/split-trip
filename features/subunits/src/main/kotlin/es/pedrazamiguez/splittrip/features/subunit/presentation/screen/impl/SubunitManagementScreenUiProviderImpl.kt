package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.SquareRoundedPlus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackFeatureGateBlocked
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY

class SubunitManagementScreenUiProviderImpl(
    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase,
    private val featureGateService: FeatureGateService,
    private val upgradeNavigator: UpgradeNavigator,
    private val telemetryTracker: TelemetryTracker,
    override val route: String = Routes.MANAGE_SUBUNITS
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        DynamicTopAppBar(
            title = stringResource(R.string.subunit_manage_title),
            onBack = { navController.popBackStack() }
        )
    }

    override val mainAction: MainAction?
        @Composable
        get() {
            val selectedGroup by observeSelectedGroupUseCase().collectAsStateWithLifecycle(initialValue = null)
            if (!shouldShowMainAction(selectedGroup?.status)) return null

            val navController = LocalTabNavController.current
            val rootNavController = LocalRootNavController.current
            val pillController = LocalTopPillController.current
            val backStackEntry by navController.currentBackStackEntryAsState()
            val groupId = backStackEntry?.arguments?.getString("groupId") ?: return null
            val isSubunitCreationEnabled by featureGateService
                .isFeatureEnabled(GatedFeature.SUBUNIT_CREATION, groupId)
                .collectAsStateWithLifecycle(initialValue = true)
            val isActingUserPro by featureGateService
                .isActingUserPro()
                .collectAsStateWithLifecycle(initialValue = false)
            val proRequiredMessage = stringResource(R.string.subunit_error_pro_required)
            var showProPreviewSheet by remember { mutableStateOf(false) }

            if (showProPreviewSheet) {
                ProFeaturePreviewBottomSheet(
                    previewType = ProFeaturePreviewType.SUBUNIT_CREATION,
                    onUpgrade = {
                        showProPreviewSheet = false
                        upgradeNavigator.navigateToUpgrade(
                            rootNavController,
                            source = MonetizationTelemetry.EntrySource.SUBUNIT_MANAGEMENT
                        )
                    },
                    onDismiss = { showProPreviewSheet = false }
                )
            }

            return MainAction(
                icon = TablerIcons.Outline.SquareRoundedPlus,
                contentDescription = stringResource(R.string.subunit_create),
                onClick = {
                    handleMainActionClick(
                        isSubunitCreationEnabled = isSubunitCreationEnabled,
                        isActingUserPro = isActingUserPro,
                        groupId = groupId,
                        proRequiredMessage = proRequiredMessage,
                        navigateToCreate = { targetRoute -> navController.navigate(targetRoute) },
                        showProPreview = { showProPreviewSheet = true },
                        showNotification = { message -> pillController.showPill(message) }
                    )
                },
                sharedTransitionKey = if (isSubunitCreationEnabled) CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY else null,
                showProBadge = shouldShowProBadge(
                    isSubunitCreationEnabled = isSubunitCreationEnabled,
                    isActingUserPro = isActingUserPro
                )
            )
        }

    internal fun handleMainActionClick(
        isSubunitCreationEnabled: Boolean,
        isActingUserPro: Boolean,
        groupId: String,
        proRequiredMessage: String,
        navigateToCreate: (String) -> Unit,
        showProPreview: () -> Unit,
        showNotification: (String) -> Unit
    ) {
        if (isSubunitCreationEnabled) {
            navigateToCreate(Routes.createEditSubunitRoute(groupId))
        } else {
            telemetryTracker.trackFeatureGateBlocked(
                feature = GatedFeature.SUBUNIT_CREATION,
                entryPoint = MonetizationTelemetry.GateEntryPoint.SUBUNIT_FAB,
                upgradePrompted = !isActingUserPro
            )
            if (isActingUserPro) {
                showNotification(proRequiredMessage)
            } else {
                showProPreview()
            }
        }
    }

    internal fun shouldShowMainAction(status: GroupStatus?): Boolean =
        status != GroupStatus.ARCHIVED

    internal fun shouldShowProBadge(
        isSubunitCreationEnabled: Boolean,
        isActingUserPro: Boolean
    ): Boolean = !isSubunitCreationEnabled && !isActingUserPro
}
