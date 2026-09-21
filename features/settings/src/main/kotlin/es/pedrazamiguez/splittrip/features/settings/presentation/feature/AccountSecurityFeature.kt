package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.biometric.BiometricPromptConfig
import es.pedrazamiguez.splittrip.core.designsystem.biometric.BiometricPromptHelper
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
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
            when (action) {
                is AccountSecurityUiAction.ShowTopPill -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is AccountSecurityUiAction.NavigateToRoute -> {
                    navController.navigate(action.route)
                }
                AccountSecurityUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
                AccountSecurityUiAction.RequestBiometricConfirmation -> {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        BiometricPromptHelper.authenticate(
                            activity = activity,
                            config = BiometricPromptConfig(
                                title = confirmTitle,
                                subtitle = confirmSubtitle,
                                negativeButtonText = confirmNegative
                            ),
                            onSuccess = {
                                viewModel.onEvent(AccountSecurityUiEvent.BiometricConfirmationSuccess)
                            },
                            onError = { errorCode, _ ->
                                when (errorCode) {
                                    BiometricPrompt.ERROR_USER_CANCELED,
                                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                    BiometricPrompt.ERROR_CANCELED -> {
                                        // Standard user cancellations — do not show error pill
                                    }
                                    else -> {
                                        pillController.showPill(genericError)
                                    }
                                }
                            }
                        )
                    }
                }
            }
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
