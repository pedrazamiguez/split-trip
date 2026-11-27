package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.biometric.BiometricPromptConfig
import es.pedrazamiguez.splittrip.core.designsystem.biometric.BiometricPromptHelper
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountSecurityUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.AccountSecurityScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AccountSecurityViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountSecurityUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountSecurityUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun AccountSecurityFeature(
    viewModel: AccountSecurityViewModel = koinViewModel<AccountSecurityViewModel>(),
    accountSecurityUiMapper: AccountSecurityUiMapper = koinInject<AccountSecurityUiMapper>()
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val navController = LocalRootNavController.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val confirmTitle = stringResource(DesignSystemR.string.biometric_prompt_confirm_title)
    val confirmSubtitle = stringResource(DesignSystemR.string.biometric_prompt_confirm_subtitle)
    val confirmNegative = stringResource(DesignSystemR.string.biometric_prompt_negative)
    val genericError = stringResource(DesignSystemR.string.biometric_auth_error_generic)

    LaunchedEffect(confirmTitle, confirmSubtitle, confirmNegative, genericError) {
        viewModel.actions.collectLatest { action ->
            handleAccountSecurityAction(
                action = action,
                context = context,
                pillController = pillController,
                navController = navController,
                biometricConfig = BiometricPromptConfig(
                    title = confirmTitle,
                    subtitle = confirmSubtitle,
                    negativeButtonText = confirmNegative
                ),
                genericError = genericError,
                onBiometricSuccess = {
                    viewModel.onEvent(AccountSecurityUiEvent.BiometricConfirmationSuccess)
                }
            )
        }
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS_SECURITY) {
        AccountSecurityScreen(
            uiState = uiState,
            uiMapper = accountSecurityUiMapper,
            onEvent = viewModel::onEvent
        )
    }
}

@Suppress("LongParameterList")
private fun handleAccountSecurityAction(
    action: AccountSecurityUiAction,
    context: Context,
    pillController: TopPillController,
    navController: NavController,
    biometricConfig: BiometricPromptConfig,
    genericError: String,
    onBiometricSuccess: () -> Unit
) {
    when (action) {
        is AccountSecurityUiAction.ShowTopPill -> {
            pillController.showPill(action.message.asString(context), action.style)
        }
        is AccountSecurityUiAction.NavigateToRoute -> {
            navController.navigate(action.route)
        }
        AccountSecurityUiAction.NavigateBack -> {
            navController.popBackStack()
        }
        AccountSecurityUiAction.RequestBiometricConfirmation -> {
            val activity = context as? FragmentActivity ?: return
            BiometricPromptHelper.authenticate(
                activity = activity,
                config = biometricConfig,
                onSuccess = onBiometricSuccess,
                onError = { errorCode, _ ->
                    val isUserCancel = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    if (!isUserCancel) {
                        pillController.showPill(genericError, TopPillStyle.ERROR)
                    }
                }
            )
        }
    }
}
