package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.permission.checkNotificationPermission
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestNotificationPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.SettingsScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsFeature(
    navController: NavHostController = LocalRootNavController.current,
    settingsViewModel: SettingsViewModel = koinViewModel<SettingsViewModel>()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentCurrency by settingsViewModel.currentCurrency.collectAsStateWithLifecycle()
    val hasPermission by settingsViewModel.hasNotificationPermission.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Update permission state when screen is resumed
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            settingsViewModel.updateNotificationPermission(checkNotificationPermission(context))
        }
    }

    val requestPermission = rememberRequestNotificationPermission { isGranted ->
        settingsViewModel.updateNotificationPermission(isGranted)
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS) {
        SettingsScreen(
            onNotificationsClick = {
                if (hasPermission) {
                    navController.navigate(Routes.SETTINGS_NOTIFICATIONS)
                } else {
                    requestPermission()
                }
            },
            onNotificationSwitchToggle = {
                if (hasPermission) {
                    // Permission already granted — open system settings to allow user to revoke
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } else {
                    requestPermission()
                }
            },
            hasNotificationPermission = hasPermission,
            currentCurrency = currentCurrency,
            onDefaultCurrencyClick = {
                navController.navigate(Routes.SETTINGS_DEFAULT_CURRENCY)
            },
            onLogoutClick = { showLogoutDialog = true }
        )
    }

    if (showLogoutDialog) {
        DestructiveConfirmationDialog(
            title = stringResource(R.string.settings_logout_dialog_title),
            text = stringResource(R.string.settings_logout_dialog_text),
            confirmLabel = stringResource(R.string.settings_logout_dialog_confirm),
            onConfirm = {
                showLogoutDialog = false
                settingsViewModel.signOut {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}
