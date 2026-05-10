package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.presentation.component.LogoutButton
import es.pedrazamiguez.splittrip.features.settings.presentation.component.settingsSections
import es.pedrazamiguez.splittrip.features.settings.presentation.data.buildSettingsSections

@Composable
fun SettingsScreen(
    onNotificationsClick: () -> Unit = {},
    onNotificationSwitchToggle: () -> Unit = {},
    hasNotificationPermission: Boolean = false,
    currentCurrency: Currency? = null,
    onDefaultCurrencyClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val sections = buildSettingsSections(
        onNotificationsClick = onNotificationsClick,
        onNotificationSwitchToggle = onNotificationSwitchToggle,
        hasNotificationPermission = hasNotificationPermission,
        currentCurrency = currentCurrency,
        onDefaultCurrencyClick = onDefaultCurrencyClick
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        settingsSections(sections)

        item(key = "logout_button") {
            LogoutButton { onLogoutClick() }
        }
    }
}
