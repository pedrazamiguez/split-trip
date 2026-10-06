package es.pedrazamiguez.splittrip.features.settings.presentation.data

import es.pedrazamiguez.splittrip.domain.enums.Currency

data class SettingsPreferencesParams(
    val onNotificationsClick: () -> Unit,
    val onNotificationSwitchToggle: () -> Unit,
    val hasNotificationPermission: Boolean,
    val currentCurrency: Currency?,
    val onDefaultCurrencyClick: () -> Unit,
    val currentLanguageCode: String,
    val onLanguageClick: () -> Unit,
    val currentThemeCode: String,
    val onThemeClick: () -> Unit,
    val onAccountStatusClick: () -> Unit,
    val onSubscriptionsClick: () -> Unit,
    val onAccountSecurityClick: () -> Unit,
    val onBugReportClick: () -> Unit,
    val onFeatureSuggestionClick: () -> Unit,
    val onFaqClick: () -> Unit,
    val onContactSupportClick: () -> Unit,
    val onPrivacyPolicyClick: () -> Unit,
    val onOpenSourceClick: () -> Unit,
    val onDeveloperInfoClick: () -> Unit,
    val onCopyDiagnosticsClick: () -> Unit
)
