package es.pedrazamiguez.splittrip

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.BRANDED_LOADING_SCREEN_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.security.APP_LOCK_GATE_TEST_TAG
import es.pedrazamiguez.splittrip.di.createAppNavHostTestModule
import es.pedrazamiguez.splittrip.domain.enums.AppTheme
import es.pedrazamiguez.splittrip.domain.enums.BiometricCapability
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetAppThemeUseCase
import es.pedrazamiguez.splittrip.features.main.navigation.DeepLinkHolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun startTestKoin(
        navModule: Module,
        themeFlow: Flow<String> = flowOf(AppTheme.SYSTEM.code)
    ) {
        val themeModule = module {
            factory<GetAppThemeUseCase> {
                mockk<GetAppThemeUseCase>().apply {
                    every { this@apply.invoke() } returns themeFlow
                }
            }
        }
        startKoin {
            modules(navModule, themeModule)
        }
    }

    @Test
    fun onNewIntent_nonDeepLinkDuringBrandedLoadingScreen_doesNotThrowNpe() {
        startTestKoin(
            createAppNavHostTestModule(
                authStateFlow = MutableSharedFlow()
            )
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                runCatching {
                    composeRule.onNode(hasTestTag(BRANDED_LOADING_SCREEN_TEST_TAG)).assertIsDisplayed()
                }.isSuccess
            }

            scenario.onActivity { activity ->
                activity.onNewIntent(Intent(Intent.ACTION_MAIN))
            }

            val deepLinkHolder = KoinPlatformTools.defaultContext().get().get<DeepLinkHolder>()
            assertNull(deepLinkHolder.pendingDeepLink)
        }
    }

    @Test
    fun onNewIntent_deepLinkDuringBrandedLoadingScreen_buffersWithoutThrowingNpe() {
        startTestKoin(
            createAppNavHostTestModule(
                authStateFlow = MutableSharedFlow()
            )
        )
        val deepLinkUri = Uri.parse("splittrip://groups/group-1")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                runCatching {
                    composeRule.onNode(hasTestTag(BRANDED_LOADING_SCREEN_TEST_TAG)).assertIsDisplayed()
                }.isSuccess
            }

            scenario.onActivity { activity ->
                activity.onNewIntent(Intent(Intent.ACTION_VIEW, deepLinkUri))
            }

            val deepLinkHolder = KoinPlatformTools.defaultContext().get().get<DeepLinkHolder>()
            assertEquals(deepLinkUri, deepLinkHolder.pendingDeepLink)
        }
    }

    @Test
    fun onNewIntent_duringAppLockGate_ignoresNonDeepLinkAndBuffersDeepLink() {
        startTestKoin(
            createAppNavHostTestModule(
                authStateFlow = flowOf(true),
                onboardingFlow = flowOf(true),
                biometricLockEnabledFlow = flowOf(true),
                biometricCapability = BiometricCapability.AVAILABLE
            )
        )
        val deepLinkUri = Uri.parse("splittrip://groups/group-2")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitUntil(timeoutMillis = 5_000L) {
                runCatching {
                    composeRule.onNode(hasTestTag(APP_LOCK_GATE_TEST_TAG)).assertIsDisplayed()
                }.isSuccess
            }

            scenario.onActivity { activity ->
                activity.onNewIntent(Intent(Intent.ACTION_MAIN))
                activity.onNewIntent(Intent(Intent.ACTION_VIEW, deepLinkUri))
            }

            composeRule.onNode(hasTestTag(APP_LOCK_GATE_TEST_TAG)).assertIsDisplayed()
            val deepLinkHolder = KoinPlatformTools.defaultContext().get().get<DeepLinkHolder>()
            assertEquals(deepLinkUri, deepLinkHolder.pendingDeepLink)
        }
    }

    @Test
    fun onCreate_withLightThemeOverride_setsLightSystemBarAppearance() {
        startTestKoin(
            navModule = createAppNavHostTestModule(authStateFlow = MutableSharedFlow()),
            themeFlow = flowOf(AppTheme.LIGHT.code)
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()

            scenario.onActivity { activity ->
                val controller = WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView
                )
                assertTrue(controller.isAppearanceLightStatusBars)
                assertTrue(controller.isAppearanceLightNavigationBars)
            }
        }
    }

    @Test
    fun onCreate_withDarkThemeOverride_setsDarkSystemBarAppearance() {
        startTestKoin(
            navModule = createAppNavHostTestModule(authStateFlow = MutableSharedFlow()),
            themeFlow = flowOf(AppTheme.DARK.code)
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()

            scenario.onActivity { activity ->
                val controller = WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView
                )
                assertFalse(controller.isAppearanceLightStatusBars)
                assertFalse(controller.isAppearanceLightNavigationBars)
            }
        }
    }

    @Test
    fun themeChange_fromLightToDark_updatesSystemBarAppearanceDynamically() {
        val themeFlow = MutableStateFlow(AppTheme.LIGHT.code)
        startTestKoin(
            navModule = createAppNavHostTestModule(authStateFlow = MutableSharedFlow()),
            themeFlow = themeFlow
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            composeRule.waitForIdle()

            scenario.onActivity { activity ->
                val controller = WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView
                )
                assertTrue(controller.isAppearanceLightStatusBars)
                assertTrue(controller.isAppearanceLightNavigationBars)
            }

            themeFlow.value = AppTheme.DARK.code
            composeRule.waitForIdle()

            scenario.onActivity { activity ->
                val controller = WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView
                )
                assertFalse(controller.isAppearanceLightStatusBars)
                assertFalse(controller.isAppearanceLightNavigationBars)
            }
        }
    }
}
