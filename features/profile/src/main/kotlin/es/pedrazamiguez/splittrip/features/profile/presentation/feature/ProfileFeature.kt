package es.pedrazamiguez.splittrip.features.profile.presentation.feature

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.ProfileScreen
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.ProfileViewModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@Composable
fun ProfileFeature(profileViewModel: ProfileViewModel = koinViewModel<ProfileViewModel>()) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val webClientId = remember(activity) { getWebClientId(activity) }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        profileViewModel.actions.collectLatest { action ->
            when (action) {
                is ProfileUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is ProfileUiAction.ShowSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                }
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onLinkGoogleClick = {
            if (!webClientId.isNullOrEmpty() && activity != null) {
                linkGoogleAccount(coroutineScope, activity, webClientId, profileViewModel, pillController)
            }
        },
        onEvent = profileViewModel::onEvent
    )
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
    profileViewModel: ProfileViewModel,
    pillController: TopPillController
) {
    coroutineScope.launch {
        try {
            val idToken = getGoogleIdToken(activity, webClientId)
            profileViewModel.onEvent(ProfileUiEvent.LinkGoogleAccount(idToken))
        } catch (_: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // User cancelled - do nothing
        } catch (e: Exception) {
            Timber.e(e, "Google account linking failed")
            pillController.showPill(message = "Google linking failed. Please try again.")
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
