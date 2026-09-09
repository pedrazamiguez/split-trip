package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.presentation.component.LogoutButton
import es.pedrazamiguez.splittrip.features.settings.presentation.component.settingsSections
import es.pedrazamiguez.splittrip.features.settings.presentation.data.SettingsPreferencesParams
import es.pedrazamiguez.splittrip.features.settings.presentation.data.buildSettingsSections

@Composable
fun SettingsScreen(
    onAccountStatusClick: () -> Unit = {},
    onSubscriptionsClick: () -> Unit = {},
    onAccountSecurityClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onNotificationSwitchToggle: () -> Unit = {},
    hasNotificationPermission: Boolean = false,
    currentCurrency: Currency? = null,
    onDefaultCurrencyClick: () -> Unit = {},
    currentLanguageCode: String = "en",
    onLanguageClick: () -> Unit = {},
    currentThemeCode: String = AppTheme.SYSTEM.code,
    onThemeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeveloperServicesTestClick: () -> Unit = {},
    onBugReportClick: () -> Unit = {},
    onFeatureSuggestionClick: () -> Unit = {},
    onFaqClick: () -> Unit = {},
    onContactSupportClick: () -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
    onOpenSourceClick: () -> Unit = {},
    onDeveloperInfoClick: () -> Unit = {}
) {
    val preferencesParams = SettingsPreferencesParams(
        onNotificationsClick = onNotificationsClick,
        onNotificationSwitchToggle = onNotificationSwitchToggle,
        hasNotificationPermission = hasNotificationPermission,
        currentCurrency = currentCurrency,
        onDefaultCurrencyClick = onDefaultCurrencyClick,
        currentLanguageCode = currentLanguageCode,
        onLanguageClick = onLanguageClick,
        currentThemeCode = currentThemeCode,
        onThemeClick = onThemeClick,
        onAccountStatusClick = onAccountStatusClick,
        onSubscriptionsClick = onSubscriptionsClick,
        onAccountSecurityClick = onAccountSecurityClick,
        onBugReportClick = onBugReportClick,
        onFeatureSuggestionClick = onFeatureSuggestionClick,
        onFaqClick = onFaqClick,
        onContactSupportClick = onContactSupportClick,
        onPrivacyPolicyClick = onPrivacyPolicyClick,
        onOpenSourceClick = onOpenSourceClick,
        onDeveloperInfoClick = onDeveloperInfoClick
    )

    val sections = buildSettingsSections(
        preferencesParams = preferencesParams,
        onServicesTestClick = onDeveloperServicesTestClick
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = MaterialTheme.spacing.Default,
            bottom = MaterialTheme.spacing.Default + LocalBottomPadding.current
        )
    ) {
        settingsSections(sections)

        item(key = "logout_button") {
            LogoutButton { onLogoutClick() }
        }
    }
}
