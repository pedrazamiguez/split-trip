package es.pedrazamiguez.splittrip.features.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.DefaultCurrencyFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.DeveloperServicesFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.LanguageFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.NotificationPreferencesFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.SettingsFeature

fun NavGraphBuilder.settingsGraph() {
    composable(Routes.SETTINGS) {
        SettingsFeature()
    }
    composable(Routes.SETTINGS_DEFAULT_CURRENCY) {
        DefaultCurrencyFeature()
    }
    composable(Routes.SETTINGS_LANGUAGE) {
        LanguageFeature()
    }
    composable(Routes.SETTINGS_NOTIFICATIONS) {
        NotificationPreferencesFeature()
    }
    composable(Routes.SETTINGS_DEVELOPER_SERVICES) {
        DeveloperServicesFeature()
    }
}
