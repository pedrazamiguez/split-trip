package es.pedrazamiguez.splittrip.features.settings.presentation.data

import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CreditCard
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Hammer
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Shield
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.SettingsItemModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SettingsData")
class SettingsDataTest {

    private var notificationsClickCount = 0
    private var notificationSwitchToggleCount = 0
    private var defaultCurrencyClickCount = 0
    private var languageClickCount = 0
    private var themeClickCount = 0
    private var accountStatusClickCount = 0
    private var subscriptionsClickCount = 0
    private var accountSecurityClickCount = 0
    private var bugReportClickCount = 0
    private var featureSuggestionClickCount = 0
    private var faqClickCount = 0
    private var contactSupportClickCount = 0
    private var privacyPolicyClickCount = 0
    private var openSourceClickCount = 0
    private var developerInfoClickCount = 0
    private var copyDiagnosticsClickCount = 0
    private var servicesTestClickCount = 0

    private fun createParams(): SettingsPreferencesParams = SettingsPreferencesParams(
        onNotificationsClick = { notificationsClickCount++ },
        onNotificationSwitchToggle = { notificationSwitchToggleCount++ },
        hasNotificationPermission = true,
        currentCurrency = Currency.EUR,
        onDefaultCurrencyClick = { defaultCurrencyClickCount++ },
        currentLanguageCode = "en",
        onLanguageClick = { languageClickCount++ },
        currentThemeCode = "system",
        onThemeClick = { themeClickCount++ },
        onAccountStatusClick = { accountStatusClickCount++ },
        onSubscriptionsClick = { subscriptionsClickCount++ },
        onAccountSecurityClick = { accountSecurityClickCount++ },
        onBugReportClick = { bugReportClickCount++ },
        onFeatureSuggestionClick = { featureSuggestionClickCount++ },
        onFaqClick = { faqClickCount++ },
        onContactSupportClick = { contactSupportClickCount++ },
        onPrivacyPolicyClick = { privacyPolicyClickCount++ },
        onOpenSourceClick = { openSourceClickCount++ },
        onDeveloperInfoClick = { developerInfoClickCount++ },
        onCopyDiagnosticsClick = { copyDiagnosticsClickCount++ }
    )

    @Nested
    @DisplayName("buildSettingsSections")
    inner class BuildSettingsSections {

        @Test
        fun `builds all five sections in expected order`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            assertEquals(5, sections.size)
            assertEquals(R.string.settings_section_account, sections[0].titleRes)
            assertEquals(R.string.settings_section_preferences, sections[1].titleRes)
            assertEquals(R.string.settings_section_developer, sections[2].titleRes)
            assertEquals(R.string.settings_section_support, sections[3].titleRes)
            assertEquals(R.string.settings_section_about, sections[4].titleRes)
        }
    }

    @Nested
    @DisplayName("accountSection")
    inner class AccountSection {

        @Test
        fun `account section contains exactly three items without redundant email option`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val accountSection = sections.first { it.titleRes == R.string.settings_section_account }
            assertEquals(3, accountSection.items.size)

            val item0 = assertInstanceOf(SettingsItemModel.Standard::class.java, accountSection.items[0])
            assertEquals(R.string.settings_account_status_title, item0.titleRes)
            assertEquals(R.string.settings_account_status_description, item0.descriptionRes)
            assertEquals(TablerIcons.Outline.InfoCircle, item0.icon)

            val item1 = assertInstanceOf(SettingsItemModel.Standard::class.java, accountSection.items[1])
            assertEquals(R.string.settings_account_subscriptions_title, item1.titleRes)
            assertEquals(R.string.settings_account_subscriptions_description, item1.descriptionRes)
            assertEquals(TablerIcons.Outline.CreditCard, item1.icon)

            val item2 = assertInstanceOf(SettingsItemModel.Standard::class.java, accountSection.items[2])
            assertEquals(R.string.settings_account_security_title, item2.titleRes)
            assertEquals(R.string.settings_account_security_description, item2.descriptionRes)
            assertEquals(TablerIcons.Outline.Shield, item2.icon)
        }

        @Test
        fun `account status item click delegates to onAccountStatusClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val accountSection = sections.first { it.titleRes == R.string.settings_section_account }
            val statusItem = assertInstanceOf(SettingsItemModel.Standard::class.java, accountSection.items[0])

            assertEquals(0, accountStatusClickCount)
            statusItem.onClick?.invoke()
            assertEquals(1, accountStatusClickCount)
        }

        @Test
        fun `subscriptions item click delegates to onSubscriptionsClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val accountSection = sections.first { it.titleRes == R.string.settings_section_account }
            val subscriptionsItem = assertInstanceOf(SettingsItemModel.Standard::class.java, accountSection.items[1])

            assertEquals(0, subscriptionsClickCount)
            subscriptionsItem.onClick?.invoke()
            assertEquals(1, subscriptionsClickCount)
        }

        @Test
        fun `account security item click delegates to onAccountSecurityClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val accountSection = sections.first { it.titleRes == R.string.settings_section_account }
            val securityItem = assertInstanceOf(SettingsItemModel.Standard::class.java, accountSection.items[2])

            assertEquals(0, accountSecurityClickCount)
            securityItem.onClick?.invoke()
            assertEquals(1, accountSecurityClickCount)
        }
    }

    @Nested
    @DisplayName("developerSection")
    inner class DeveloperSection {

        @Test
        fun `developer section contains exactly one item for developer services`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val developerSection = sections.first { it.titleRes == R.string.settings_section_developer }
            assertEquals(1, developerSection.items.size)

            val item0 = assertInstanceOf(SettingsItemModel.Standard::class.java, developerSection.items[0])
            assertEquals(R.string.settings_developer_services_title, item0.titleRes)
            assertEquals(R.string.settings_developer_services_description, item0.descriptionRes)
            assertEquals(TablerIcons.Outline.Hammer, item0.icon)
        }

        @Test
        fun `developer services item click delegates to onServicesTestClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val developerSection = sections.first { it.titleRes == R.string.settings_section_developer }
            val servicesItem = developerSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_developer_services_title }

            assertEquals(0, servicesTestClickCount)
            servicesItem.onClick?.invoke()
            assertEquals(1, servicesTestClickCount)
        }
    }

    @Nested
    @DisplayName("preferencesSection")
    inner class PreferencesSection {

        @Test
        fun `theme item click delegates to onThemeClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val preferencesSection = sections.first { it.titleRes == R.string.settings_section_preferences }
            val themeItem = preferencesSection.items
                .filterIsInstance<SettingsItemModel.WithCustomDescription>()
                .first { it.titleRes == R.string.settings_preferences_theme_title }

            assertEquals(0, themeClickCount)
            themeItem.onClick?.invoke()
            assertEquals(1, themeClickCount)
        }

        @Test
        fun `language item click delegates to onLanguageClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val preferencesSection = sections.first { it.titleRes == R.string.settings_section_preferences }
            val languageItem = preferencesSection.items
                .filterIsInstance<SettingsItemModel.WithCustomDescription>()
                .first { it.titleRes == R.string.settings_preferences_language_title }

            assertEquals(0, languageClickCount)
            languageItem.onClick?.invoke()
            assertEquals(1, languageClickCount)
        }

        @Test
        fun `notifications item click delegates to onNotificationsClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val preferencesSection = sections.first { it.titleRes == R.string.settings_section_preferences }
            val notifItem = preferencesSection.items
                .filterIsInstance<SettingsItemModel.WithTrailing>()
                .first { it.titleRes == R.string.settings_preferences_notifications_title }

            assertEquals(0, notificationsClickCount)
            notifItem.onClick?.invoke()
            assertEquals(1, notificationsClickCount)
        }

        @Test
        fun `currency item click delegates to onDefaultCurrencyClick callback`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val preferencesSection = sections.first { it.titleRes == R.string.settings_section_preferences }
            val currencyItem = preferencesSection.items
                .filterIsInstance<SettingsItemModel.WithCustomDescription>()
                .first { it.titleRes == R.string.settings_preferences_currency_title }

            assertEquals(0, defaultCurrencyClickCount)
            currencyItem.onClick?.invoke()
            assertEquals(1, defaultCurrencyClickCount)
        }
    }

    @Nested
    @DisplayName("supportSection")
    inner class SupportSection {

        @Test
        fun `support callbacks delegate properly`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val supportSection = sections.first { it.titleRes == R.string.settings_section_support }
            val bugItem = supportSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_support_bug_title }
            val featureItem = supportSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_support_feature_title }
            val faqItem = supportSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_support_faq_title }
            val supportItem = supportSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_support_support_title }

            assertEquals(0, bugReportClickCount)
            bugItem.onClick?.invoke()
            assertEquals(1, bugReportClickCount)

            assertEquals(0, featureSuggestionClickCount)
            featureItem.onClick?.invoke()
            assertEquals(1, featureSuggestionClickCount)

            assertEquals(0, faqClickCount)
            faqItem.onClick?.invoke()
            assertEquals(1, faqClickCount)

            assertEquals(0, contactSupportClickCount)
            supportItem.onClick?.invoke()
            assertEquals(1, contactSupportClickCount)
        }
    }

    @Nested
    @DisplayName("aboutSection")
    inner class AboutSection {

        @Test
        fun `about callbacks delegate properly`() {
            val params = createParams()
            val sections = buildSettingsSections(
                preferencesParams = params,
                onServicesTestClick = { servicesTestClickCount++ }
            )

            val aboutSection = sections.first { it.titleRes == R.string.settings_section_about }
            val privacyItem = aboutSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_about_privacy_title }
            val openSourceItem = aboutSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_about_libraries_title }
            val developerItem = aboutSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_about_developer_title }
            val copyDiagnosticsItem = aboutSection.items
                .filterIsInstance<SettingsItemModel.Standard>()
                .first { it.titleRes == R.string.settings_developer_diagnostics_title }

            assertEquals(0, privacyPolicyClickCount)
            privacyItem.onClick?.invoke()
            assertEquals(1, privacyPolicyClickCount)

            assertEquals(0, openSourceClickCount)
            openSourceItem.onClick?.invoke()
            assertEquals(1, openSourceClickCount)

            assertEquals(0, developerInfoClickCount)
            developerItem.onClick?.invoke()
            assertEquals(1, developerInfoClickCount)

            assertEquals(0, copyDiagnosticsClickCount)
            copyDiagnosticsItem.onClick?.invoke()
            assertEquals(1, copyDiagnosticsClickCount)
        }
    }
}
