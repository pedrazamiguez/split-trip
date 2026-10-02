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
import es.pedrazamiguez.splittrip.core.common.provider.SupportEmail
import es.pedrazamiguez.splittrip.core.common.provider.SupportEmailProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.permission.checkNotificationPermission
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestNotificationPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.SettingsScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Suppress("LongMethod", "CognitiveComplexMethod")
@Composable
fun SettingsFeature(
    navController: NavHostController = LocalRootNavController.current,
    settingsViewModel: SettingsViewModel = koinViewModel<SettingsViewModel>()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pillController = LocalTopPillController.current
    val supportEmailProvider = koinInject<SupportEmailProvider>()

    val currentCurrency by settingsViewModel.currentCurrency.collectAsStateWithLifecycle()
    val hasPermission by settingsViewModel.hasNotificationPermission.collectAsStateWithLifecycle()
    val currentLanguageCode by settingsViewModel.currentLanguageCode.collectAsStateWithLifecycle()
    val shouldShowLanguagePill by settingsViewModel.shouldShowLanguagePill.collectAsStateWithLifecycle()
    val currentThemeCode by settingsViewModel.currentThemeCode.collectAsStateWithLifecycle()
    val isAnonymous by settingsViewModel.isAnonymous.collectAsStateWithLifecycle()

    var showLogoutDialog by remember { mutableStateOf(false) }

    // Update permission state when screen is resumed
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            settingsViewModel.updateNotificationPermission(checkNotificationPermission(context))
        }
    }

    val languageEs = stringResource(R.string.settings_preferences_language_es)
    val languageEn = stringResource(R.string.settings_preferences_language_en)
    val languageAndaluz = stringResource(R.string.settings_preferences_language_andaluz)
    val languageChangedFormat = stringResource(R.string.settings_preferences_language_changed_format)

    LaunchedEffect(shouldShowLanguagePill) {
        if (shouldShowLanguagePill) {
            val languageName = when (AppLanguage.fromCode(currentLanguageCode)) {
                AppLanguage.ES -> languageEs
                AppLanguage.EN -> languageEn
                AppLanguage.ANDALUZ -> languageAndaluz
            }
            pillController.showPill(
                languageChangedFormat.format(languageName)
            )
            settingsViewModel.consumeLanguagePill()
        }
    }

    val requestPermission = rememberRequestNotificationPermission { isGranted ->
        settingsViewModel.updateNotificationPermission(isGranted)
    }

    val sendSupportEmail: (SupportEmail) -> Unit = { email ->
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email.recipient))
            putExtra(Intent.EXTRA_SUBJECT, email.subject)
            putExtra(Intent.EXTRA_TEXT, email.body)
        }
        context.startActivity(Intent.createChooser(intent, "Send email..."))
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS, scrollContentUnderTopBar = true) {
        SettingsScreen(
            onAccountStatusClick = { navController.navigate(Routes.SETTINGS_ACCOUNT_STATUS) },
            onSubscriptionsClick = { navController.navigate(Routes.SETTINGS_SUBSCRIPTIONS) },
            onAccountSecurityClick = { navController.navigate(Routes.SETTINGS_SECURITY) },
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
            onLanguageClick = { navController.navigate(Routes.SETTINGS_LANGUAGE) },
            currentThemeCode = currentThemeCode,
            onThemeClick = { navController.navigate(Routes.SETTINGS_THEME) },
            onLogoutClick = { showLogoutDialog = true },
            onDeveloperServicesTestClick = {
                navController.navigate(Routes.SETTINGS_DEVELOPER_SERVICES)
            },
            onBugReportClick = {
                sendSupportEmail(supportEmailProvider.buildBugReportEmail())
            },
            onFeatureSuggestionClick = {
                sendSupportEmail(supportEmailProvider.buildFeatureSuggestionEmail())
            },
            onFaqClick = {
                navController.navigate(Routes.SETTINGS_FAQ)
            },
            onContactSupportClick = {
                sendSupportEmail(supportEmailProvider.buildContactSupportEmail())
            },
            onPrivacyPolicyClick = {
                navController.navigate(Routes.SETTINGS_PRIVACY_POLICY)
            },
            onOpenSourceClick = {
                navController.navigate(Routes.SETTINGS_OPEN_SOURCE)
            },
            onDeveloperInfoClick = {
                navController.navigate(Routes.SETTINGS_DEVELOPER_INFO)
            }
        )
    }

    if (showLogoutDialog) {
        val title = if (isAnonymous) {
            stringResource(R.string.settings_logout_guest_dialog_title)
        } else {
            stringResource(R.string.settings_logout_dialog_title)
        }
        val text = if (isAnonymous) {
            stringResource(R.string.settings_logout_guest_dialog_text)
        } else {
            stringResource(R.string.settings_logout_dialog_text)
        }
        DestructiveConfirmationDialog(
            title = title,
            text = text,
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
