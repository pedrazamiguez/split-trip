package es.pedrazamiguez.splittrip.features.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.AccountSecurityFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.AccountStatusFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.DefaultCurrencyFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.DeveloperInfoFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.DeveloperServicesFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.FaqFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.LanguageFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.NotificationPreferencesFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.OpenSourceFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.PlanAllowancesFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.PrivacyPolicyFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.SettingsFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.SubscriptionsFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.ThemeFeature

fun NavGraphBuilder.settingsGraph() {
    composable(Routes.SETTINGS) {
        SettingsFeature()
    }
    composable(Routes.SETTINGS_ACCOUNT_STATUS) {
        AccountStatusFeature()
    }
    composable(Routes.SETTINGS_PLAN_ALLOWANCES) {
        PlanAllowancesFeature()
    }
    composable(
        route = Routes.SETTINGS_SUBSCRIPTIONS_ROUTE,
        arguments = listOf(
            navArgument(Routes.SETTINGS_SUBSCRIPTIONS_ARG_SOURCE) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val entrySource = backStackEntry.arguments?.getString(Routes.SETTINGS_SUBSCRIPTIONS_ARG_SOURCE)
        SubscriptionsFeature(entrySource = entrySource)
    }
    composable(Routes.SETTINGS_SECURITY) {
        AccountSecurityFeature()
    }
    composable(Routes.SETTINGS_DEFAULT_CURRENCY) {
        DefaultCurrencyFeature()
    }
    composable(Routes.SETTINGS_LANGUAGE) {
        LanguageFeature()
    }
    composable(Routes.SETTINGS_THEME) {
        ThemeFeature()
    }
    composable(Routes.SETTINGS_NOTIFICATIONS) {
        NotificationPreferencesFeature()
    }
    composable(Routes.SETTINGS_DEVELOPER_SERVICES) {
        DeveloperServicesFeature()
    }
    composable(Routes.SETTINGS_FAQ) {
        FaqFeature()
    }
    composable(Routes.SETTINGS_PRIVACY_POLICY) {
        PrivacyPolicyFeature()
    }
    composable(Routes.SETTINGS_OPEN_SOURCE) {
        OpenSourceFeature()
    }
    composable(Routes.SETTINGS_DEVELOPER_INFO) {
        DeveloperInfoFeature()
    }
}
