package es.pedrazamiguez.splittrip.features.group.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersPlus
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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.LocalProfileAvatarUrl
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackLimitGateBlocked
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CREATE_EDIT_GROUP_SHARED_ELEMENT_KEY

class GroupsScreenUiProviderImpl(
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val authenticationService: AuthenticationService,
    private val featureGateService: FeatureGateService,
    private val upgradeNavigator: UpgradeNavigator,
    private val telemetryTracker: TelemetryTracker,
    override val route: String = Routes.GROUPS
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val rootNavController = LocalRootNavController.current
        val avatarUrl = LocalProfileAvatarUrl.current

        DynamicTopAppBar(
            title = stringResource(R.string.groups_title),
            subtitle = stringResource(R.string.groups_subtitle),
            actions = {
                ProfileAvatarButton(
                    avatarUrl = avatarUrl,
                    onClick = { rootNavController.navigate(Routes.PROFILE) }
                )
            }
        )
    }

    override val mainAction: MainAction?
        @Composable
        get() {
            val tabNavController = LocalTabNavController.current
            val rootNavController = LocalRootNavController.current
            val pillController = LocalTopPillController.current
            val context = LocalContext.current

            val groups by getUserGroupsFlowUseCase().collectAsStateWithLifecycle(initialValue = emptyList())
            val currentUserId = authenticationService.currentUserId()
            val ownedGroupsCount = groups.count { it.createdBy == currentUserId }

            val limitResult by featureGateService
                .checkLimit(GatedLimit.MAX_OWNED_GROUPS_COUNT, ownedGroupsCount)
                .collectAsStateWithLifecycle(initialValue = LimitResult.Allowed)
            val isActingUserPro by featureGateService
                .isActingUserPro()
                .collectAsStateWithLifecycle(initialValue = false)

            val isAllowed = limitResult is LimitResult.Allowed
            val limitReachedMessage = UiText.StringResource(R.string.group_error_limit_groups_exceeded)
            var showProPreviewSheet by remember { mutableStateOf(false) }

            if (showProPreviewSheet) {
                ProFeaturePreviewBottomSheet(
                    previewType = ProFeaturePreviewType.UNLIMITED_GROUPS,
                    onUpgrade = {
                        showProPreviewSheet = false
                        upgradeNavigator.navigateToUpgrade(
                            rootNavController,
                            source = MonetizationTelemetry.EntrySource.GROUPS_FAB
                        )
                    },
                    onDismiss = { showProPreviewSheet = false }
                )
            }

            return MainAction(
                icon = TablerIcons.Outline.UsersPlus,
                contentDescription = stringResource(R.string.groups_create),
                onClick = {
                    handleMainActionClick(
                        isAllowed = isAllowed,
                        isActingUserPro = isActingUserPro,
                        limitReachedMessage = limitReachedMessage,
                        navigateToCreateGroup = { tabNavController.navigate(Routes.CREATE_GROUP) },
                        showProPreview = { showProPreviewSheet = true },
                        showNotification = { message -> pillController.showPill(message.asString(context)) }
                    )
                },
                sharedTransitionKey = if (isAllowed) CREATE_EDIT_GROUP_SHARED_ELEMENT_KEY else null,
                showProBadge = shouldShowProBadge(
                    isAllowed = isAllowed,
                    isActingUserPro = isActingUserPro
                )
            )
        }

    internal fun handleMainActionClick(
        isAllowed: Boolean,
        isActingUserPro: Boolean,
        limitReachedMessage: UiText,
        navigateToCreateGroup: () -> Unit,
        showProPreview: () -> Unit,
        showNotification: (UiText) -> Unit
    ) {
        if (isAllowed) {
            navigateToCreateGroup()
        } else {
            telemetryTracker.trackLimitGateBlocked(
                limit = GatedLimit.MAX_OWNED_GROUPS_COUNT,
                entryPoint = MonetizationTelemetry.GateEntryPoint.GROUPS_FAB,
                upgradePrompted = !isActingUserPro
            )
            if (isActingUserPro) {
                showNotification(limitReachedMessage)
            } else {
                showProPreview()
            }
        }
    }

    internal fun shouldShowProBadge(
        isAllowed: Boolean,
        isActingUserPro: Boolean
    ): Boolean = !isAllowed && !isActingUserPro
}
