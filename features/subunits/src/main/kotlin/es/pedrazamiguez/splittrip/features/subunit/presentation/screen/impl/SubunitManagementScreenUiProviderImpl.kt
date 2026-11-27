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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackLimitGateBlocked
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY

class SubunitManagementScreenUiProviderImpl(
    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase,
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
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
            val subunits by remember(groupId) {
                getGroupSubunitsFlowUseCase(groupId)
            }.collectAsStateWithLifecycle(initialValue = emptyList())
            val limitResult by remember(groupId, subunits.size) {
                featureGateService.checkLimit(
                    limit = GatedLimit.MAX_SUBUNITS_PER_GROUP,
                    currentCount = subunits.size,
                    groupId = groupId
                )
            }.collectAsStateWithLifecycle(initialValue = LimitResult.Allowed)
            val isSubunitCreationEnabled = limitResult is LimitResult.Allowed
            val limitReachedMessage = stringResource(R.string.subunit_error_limit_reached)
            var showProPreviewSheet by remember { mutableStateOf(false) }

            if (showProPreviewSheet) {
                ProFeaturePreviewBottomSheet(
                    previewType = ProFeaturePreviewType.SUBUNIT_CREATION,
                    onUpgrade = {
                        showProPreviewSheet = false
                        upgradeNavigator.navigateToUpgrade(
                            rootNavController,
                            source = MonetizationTelemetry.EntrySource.SUBUNIT_CREATION
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
                        limitResult = limitResult,
                        groupId = groupId,
                        limitReachedMessage = limitReachedMessage,
                        navigateToCreate = { targetRoute -> navController.navigate(targetRoute) },
                        showProPreview = { showProPreviewSheet = true },
                        showNotification = { message ->
                            pillController.showPill(
                                message = message,
                                style = TopPillStyle.WARNING
                            )
                        }
                    )
                },
                sharedTransitionKey = if (isSubunitCreationEnabled) CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY else null,
                showProBadge = shouldShowProBadge(limitResult)
            )
        }

    internal fun handleMainActionClick(
        limitResult: LimitResult,
        groupId: String,
        limitReachedMessage: String,
        navigateToCreate: (String) -> Unit,
        showProPreview: () -> Unit,
        showNotification: (String) -> Unit
    ) {
        when (limitResult) {
            is LimitResult.Allowed -> navigateToCreate(Routes.createEditSubunitRoute(groupId))
            is LimitResult.Blocked -> {
                telemetryTracker.trackLimitGateBlocked(
                    limit = GatedLimit.MAX_SUBUNITS_PER_GROUP,
                    entryPoint = MonetizationTelemetry.GateEntryPoint.SUBUNIT_FAB,
                    upgradePrompted = limitResult.upgradeRequired
                )
                if (limitResult.upgradeRequired) {
                    showProPreview()
                } else {
                    showNotification(limitReachedMessage)
                }
            }
        }
    }

    internal fun shouldShowMainAction(status: GroupStatus?): Boolean =
        status != GroupStatus.ARCHIVED

    internal fun shouldShowProBadge(limitResult: LimitResult): Boolean =
        limitResult is LimitResult.Blocked && limitResult.upgradeRequired
}
