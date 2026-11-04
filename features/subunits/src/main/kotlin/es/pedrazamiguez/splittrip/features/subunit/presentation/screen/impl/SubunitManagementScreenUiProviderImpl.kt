package es.pedrazamiguez.splittrip.features.subunit.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.SquareRoundedPlus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
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
            val isArchived = selectedGroup?.status == GroupStatus.ARCHIVED
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
                        navigateToSubscriptions = { upgradeNavigator.navigateToUpgrade(rootNavController) },
                        showNotification = { message -> pillController.showPill(message) }
                    )
                },
                sharedTransitionKey = if (isSubunitCreationEnabled) CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY else null,
                enabled = !isArchived,
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
        navigateToSubscriptions: () -> Unit,
        showNotification: (String) -> Unit
    ) {
        if (isSubunitCreationEnabled) {
            navigateToCreate(Routes.createEditSubunitRoute(groupId))
        } else {
            showNotification(proRequiredMessage)
            if (!isActingUserPro) {
                navigateToSubscriptions()
            }
        }
    }

    internal fun shouldShowProBadge(
        isSubunitCreationEnabled: Boolean,
        isActingUserPro: Boolean
    ): Boolean = !isSubunitCreationEnabled && !isActingUserPro
}
