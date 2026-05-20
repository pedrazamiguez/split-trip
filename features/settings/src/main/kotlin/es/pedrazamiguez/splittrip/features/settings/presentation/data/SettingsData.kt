package es.pedrazamiguez.splittrip.features.settings.presentation.data

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.getNameRes
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bell
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Book
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bug
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Bulb
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CurrencyEuro
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Hammer
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Headset
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.HelpCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Language
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Layers
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.LayoutGrid
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Mail
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.MoonStars
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ShieldLock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UserPin
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.AppVersionFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.feature.InstallationIdFeature
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsItemModel
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsSectionModel

/**
 * Builds the settings sections with all configuration.
 * This function centralizes the settings data, making it easy to:
 * - Add, remove, or reorder settings
 * - Modify settings behavior
 * - Test settings configuration
 */
@Composable
fun buildSettingsSections(
    onNotificationsClick: () -> Unit,
    onNotificationSwitchToggle: () -> Unit,
    hasNotificationPermission: Boolean,
    currentCurrency: Currency?,
    onDefaultCurrencyClick: () -> Unit,
    onServicesTestClick: () -> Unit
): List<SettingsSectionModel> = listOf(
    accountSection(),
    preferencesSection(
        onNotificationsClick = onNotificationsClick,
        onNotificationSwitchToggle = onNotificationSwitchToggle,
        hasNotificationPermission = hasNotificationPermission,
        currentCurrency = currentCurrency,
        onDefaultCurrencyClick = onDefaultCurrencyClick
    ),
    developerSection(onServicesTestClick = onServicesTestClick),
    supportSection(),
    aboutSection()
)

private fun accountSection() = SettingsSectionModel(
    titleRes = R.string.settings_section_account,
    items = listOf(
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.InfoCircle,
            titleRes = R.string.settings_account_status_title,
            descriptionRes = R.string.settings_account_status_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.CreditCard,
            titleRes = R.string.settings_account_subscriptions_title,
            descriptionRes = R.string.settings_account_subscriptions_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Shield,
            titleRes = R.string.settings_account_security_title,
            descriptionRes = R.string.settings_account_security_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Mail,
            titleRes = R.string.settings_account_email_title,
            descriptionRes = R.string.settings_account_email_description
        )
    )
)

private fun preferencesSection(
    onNotificationsClick: () -> Unit,
    onNotificationSwitchToggle: () -> Unit,
    hasNotificationPermission: Boolean,
    currentCurrency: Currency?,
    onDefaultCurrencyClick: () -> Unit
) = SettingsSectionModel(
    titleRes = R.string.settings_section_preferences,
    items = listOf(
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.MoonStars,
            titleRes = R.string.settings_preferences_theme_title,
            descriptionRes = R.string.settings_preferences_theme_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Language,
            titleRes = R.string.settings_preferences_language_title,
            descriptionRes = R.string.settings_preferences_language_description
        ),
        SettingsItemModel.WithTrailing(
            icon = TablerIcons.Outline.Bell,
            titleRes = R.string.settings_preferences_notifications_title,
            descriptionRes = R.string.settings_preferences_notifications_description,
            onClick = onNotificationsClick,
            trailingContent = {
                Switch(
                    checked = hasNotificationPermission,
                    onCheckedChange = { onNotificationSwitchToggle() }
                )
            }
        ),
        SettingsItemModel.WithCustomDescription(
            icon = TablerIcons.Outline.CurrencyEuro,
            titleRes = R.string.settings_preferences_currency_title,
            onClick = onDefaultCurrencyClick,
            descriptionContent = {
                CurrencyDescription(currentCurrency)
            }
        )
    )
)

@Composable
private fun CurrencyDescription(currentCurrency: Currency?) {
    Crossfade(
        targetState = currentCurrency,
        label = "CurrencyFade"
    ) { currency ->
        if (currency == null) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(20.dp)
            )
        } else {
            val currencyName = stringResource(id = currency.getNameRes())
            Text(text = "$currencyName (${currency.symbol})")
        }
    }
}

private fun developerSection(onServicesTestClick: () -> Unit) = SettingsSectionModel(
    titleRes = R.string.settings_section_developer,
    items = listOf(
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Layers,
            titleRes = R.string.settings_developer_layout_title,
            descriptionRes = R.string.settings_developer_layout_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.LayoutGrid,
            titleRes = R.string.settings_developer_widgets_title,
            descriptionRes = R.string.settings_developer_widgets_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Photo,
            titleRes = R.string.settings_developer_assets_title,
            descriptionRes = R.string.settings_developer_assets_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Hammer,
            titleRes = R.string.settings_developer_services_title,
            descriptionRes = R.string.settings_developer_services_description,
            onClick = onServicesTestClick
        )
    )
)

private fun supportSection() = SettingsSectionModel(
    titleRes = R.string.settings_section_support,
    items = listOf(
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Bug,
            titleRes = R.string.settings_support_bug_title,
            descriptionRes = R.string.settings_support_bug_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Bulb,
            titleRes = R.string.settings_support_feature_title,
            descriptionRes = R.string.settings_support_feature_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.HelpCircle,
            titleRes = R.string.settings_support_faq_title,
            descriptionRes = R.string.settings_support_faq_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Headset,
            titleRes = R.string.settings_support_support_title,
            descriptionRes = R.string.settings_support_support_description
        )
    )
)

private fun aboutSection() = SettingsSectionModel(
    titleRes = R.string.settings_section_about,
    items = listOf(
        SettingsItemModel.Custom { AppVersionFeature() },
        SettingsItemModel.Custom { InstallationIdFeature() },
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.ShieldLock,
            titleRes = R.string.settings_about_privacy_title,
            descriptionRes = R.string.settings_about_privacy_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.Book,
            titleRes = R.string.settings_about_libraries_title,
            descriptionRes = R.string.settings_about_libraries_description
        ),
        SettingsItemModel.Standard(
            icon = TablerIcons.Outline.UserPin,
            titleRes = R.string.settings_about_developer_title,
            descriptionRes = R.string.settings_about_developer_description
        )
    )
)
