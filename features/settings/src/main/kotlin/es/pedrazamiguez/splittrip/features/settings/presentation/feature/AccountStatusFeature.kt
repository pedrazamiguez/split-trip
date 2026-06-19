package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.AccountStatusScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.AccountStatusViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountStatusUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun AccountStatusFeature(
    viewModel: AccountStatusViewModel = koinViewModel<AccountStatusViewModel>()
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val navController = LocalRootNavController.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val webClientId = remember(activity) { getWebClientId(activity) }
    val googleLinkingFailedMsg = androidx.compose.ui.res.stringResource(
        id = R.string.account_status_error_prefix,
        "Failed to link Google"
    )

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is AccountStatusUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is AccountStatusUiAction.ShowSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                AccountStatusUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS_ACCOUNT_STATUS) {
        AccountStatusScreen(
            uiState = uiState,
            onLinkGoogleClick = {
                if (!webClientId.isNullOrEmpty() && activity != null) {
                    linkGoogleAccount(
                        coroutineScope = coroutineScope,
                        activity = activity,
                        webClientId = webClientId,
                        viewModel = viewModel,
                        pillController = pillController,
                        googleLinkingFailedMsg = googleLinkingFailedMsg
                    )
                }
            },
            onEvent = viewModel::onEvent
        )
    }

    if (uiState.showDeleteAccountDialog) {
        DestructiveConfirmationDialog(
            title = androidx.compose.ui.res.stringResource(id = R.string.account_status_delete_account_dialog_title),
            text = androidx.compose.ui.res.stringResource(id = R.string.account_status_delete_account_dialog_text),
            onConfirm = { viewModel.onEvent(AccountStatusUiEvent.ConfirmDeleteAccount) },
            onDismiss = { viewModel.onEvent(AccountStatusUiEvent.DismissDeleteAccountDialog) },
            confirmLabel = androidx.compose.ui.res.stringResource(
                id = R.string.account_status_delete_account_dialog_confirm
            )
        )
    }
}

private fun getWebClientId(activity: Activity?): String? {
    if (activity == null) return null
    val resId = activity.resources.getIdentifier(
        "default_web_client_id",
        "string",
        activity.packageName
    )
    return if (resId != 0) activity.getString(resId) else null
}

private fun linkGoogleAccount(
    coroutineScope: CoroutineScope,
    activity: Activity,
    webClientId: String,
    viewModel: AccountStatusViewModel,
    pillController: TopPillController,
    googleLinkingFailedMsg: String
) {
    coroutineScope.launch {
        try {
            val idToken = getGoogleIdToken(activity, webClientId)
            viewModel.onEvent(AccountStatusUiEvent.LinkGoogle(idToken))
        } catch (_: GetCredentialCancellationException) {
            // User cancelled - do nothing
        } catch (e: Exception) {
            Timber.e(e, "Google account linking failed")
            pillController.showPill(message = googleLinkingFailedMsg)
        }
    }
}

private suspend fun getGoogleIdToken(activity: Activity, webClientId: String): String {
    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    val credentialManager = CredentialManager.create(activity)
    val result = credentialManager.getCredential(
        request = request,
        context = activity
    )

    return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
}
