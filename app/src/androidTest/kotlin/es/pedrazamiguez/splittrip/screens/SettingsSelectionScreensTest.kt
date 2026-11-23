package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.extension.getNameRes
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.domain.enums.AppLanguage
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.enums.Currency
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DefaultCurrencyScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.LanguageScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.ThemeScreen
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for [ThemeScreen], [LanguageScreen], and [DefaultCurrencyScreen].
 */
@RunWith(AndroidJUnit4::class)
class SettingsSelectionScreensTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun themeScreen_rendersThemesAndInvokesCallbackOnSelection() {
        val systemThemeText = context.getString(R.string.settings_preferences_theme_system)
        val darkThemeText = context.getString(R.string.settings_preferences_theme_dark)
        var selectedThemeCode: String? = null

        composeRule.setContent {
            SplitTripTheme {
                ThemeScreen(
                    availableThemes = AppTheme.entries,
                    selectedThemeCode = AppTheme.SYSTEM.code,
                    onThemeSelected = { selectedThemeCode = it }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(systemThemeText).assertIsDisplayed()
        composeRule.onNodeWithText(AppTheme.SYSTEM.englishName).assertIsDisplayed()
        composeRule.onNodeWithText(darkThemeText).assertIsDisplayed()
        composeRule.onNodeWithText(AppTheme.DARK.englishName).assertIsDisplayed()

        composeRule.onNodeWithText(darkThemeText).performClick()
        composeRule.waitForIdle()

        assertEquals("dark", selectedThemeCode)
    }

    @Test
    fun languageScreen_rendersLanguagesWithBadgesAndInvokesCallbackOnSelection() {
        val spanishText = context.getString(R.string.settings_preferences_language_es)
        var selectedLanguageCode: String? = null

        composeRule.setContent {
            SplitTripTheme {
                LanguageScreen(
                    availableLanguages = AppLanguage.entries,
                    selectedLanguageCode = AppLanguage.EN.code,
                    onLanguageSelected = { selectedLanguageCode = it }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("EN").assertIsDisplayed()
        composeRule.onNodeWithText("ES").assertIsDisplayed()
        composeRule.onNodeWithText("AN").assertIsDisplayed()
        composeRule.onNodeWithText(spanishText).assertIsDisplayed()

        composeRule.onNodeWithText(spanishText).performClick()
        composeRule.waitForIdle()

        assertEquals("es", selectedLanguageCode)
    }

    @Test
    fun defaultCurrencyScreen_rendersCurrenciesWithSymbolsAndInvokesCallbackOnSelection() {
        val eurName = context.getString(Currency.EUR.getNameRes())
        val usdName = context.getString(Currency.USD.getNameRes())
        var selectedCurrencyCode: String? = null

        composeRule.setContent {
            SplitTripTheme {
                DefaultCurrencyScreen(
                    availableCurrencies = listOf(Currency.EUR, Currency.USD, Currency.GBP),
                    selectedCurrencyCode = Currency.EUR.name,
                    onCurrencySelected = { selectedCurrencyCode = it }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(eurName).assertIsDisplayed()
        composeRule.onNodeWithText(Currency.EUR.symbol).assertIsDisplayed()
        composeRule.onNodeWithText(usdName).assertIsDisplayed()
        composeRule.onNodeWithText(Currency.USD.symbol).assertIsDisplayed()
        composeRule.onNodeWithText(Currency.GBP.symbol).assertIsDisplayed()

        composeRule.onNodeWithText(usdName).performClick()
        composeRule.waitForIdle()

        assertEquals("USD", selectedCurrencyCode)
    }
}
