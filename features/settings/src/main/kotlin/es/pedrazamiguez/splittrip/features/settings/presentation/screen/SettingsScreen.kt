package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.presentation.component.LogoutButton
import es.pedrazamiguez.splittrip.features.settings.presentation.component.settingsSections
import es.pedrazamiguez.splittrip.features.settings.presentation.data.SettingsPreferencesParams
import es.pedrazamiguez.splittrip.features.settings.presentation.data.buildSettingsSections

@Composable
fun SettingsScreen(
    onNotificationsClick: () -> Unit = {},
    onNotificationSwitchToggle: () -> Unit = {},
    hasNotificationPermission: Boolean = false,
    currentCurrency: Currency? = null,
    onDefaultCurrencyClick: () -> Unit = {},
    currentLanguageCode: String = "en",
    onLanguageClick: () -> Unit = {},
    currentThemeCode: String? = null,
    onThemeClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeveloperServicesTestClick: () -> Unit = {}
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
        onThemeClick = onThemeClick
    )

    val sections = buildSettingsSections(
        preferencesParams = preferencesParams,
        onServicesTestClick = onDeveloperServicesTestClick
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.Default)
    ) {
        settingsSections(sections)

        item(key = "logout_button") {
            LogoutButton { onLogoutClick() }
        }
    }
}
