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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.domain.enums.Currency
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
    val pillController = LocalTopPillController.current

    val currentCurrency by settingsViewModel.currentCurrency.collectAsStateWithLifecycle()
    val hasPermission by settingsViewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    val currentLanguageCode by settingsViewModel.currentLanguageCode.collectAsStateWithLifecycle()
    val shouldShowLanguagePill by settingsViewModel.shouldShowLanguagePill.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Update permission state when screen is resumed
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            settingsViewModel.updateNotificationPermission(checkNotificationPermission(context))
        }
    }

    LaunchedEffect(shouldShowLanguagePill) {
        if (shouldShowLanguagePill) {
            val languageName = when (currentLanguageCode) {
                "es" -> context.getString(R.string.settings_preferences_language_es)
                else -> context.getString(R.string.settings_preferences_language_en)
            }
            pillController.showPill(
                context.getString(
                    R.string.settings_preferences_language_changed_format,
                    languageName
                )
            )
            settingsViewModel.consumeLanguagePill()
        }
    }

    SettingsFeatureContent(
        navController = navController,
        settingsViewModel = settingsViewModel,
        hasPermission = hasPermission,
        currentCurrency = currentCurrency,
        currentLanguageCode = currentLanguageCode,
        onLanguageClick = { navController.navigate(Routes.SETTINGS_LANGUAGE) },
        onLogoutClick = { showLogoutDialog = true }
    )

    if (showLogoutDialog) {
        LogoutConfirmationDialog(
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

@Composable
private fun SettingsFeatureContent(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    hasPermission: Boolean,
    currentCurrency: Currency?,
    currentLanguageCode: String,
    onLanguageClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
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
            currentLanguageCode = currentLanguageCode,
            onLanguageClick = onLanguageClick,
            onLogoutClick = onLogoutClick,
            onDeveloperServicesTestClick = {
                navController.navigate(Routes.SETTINGS_DEVELOPER_SERVICES)
            }
        )
    }
}

@Composable
private fun LogoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DestructiveConfirmationDialog(
        title = stringResource(R.string.settings_logout_dialog_title),
        text = stringResource(R.string.settings_logout_dialog_text),
        confirmLabel = stringResource(R.string.settings_logout_dialog_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
