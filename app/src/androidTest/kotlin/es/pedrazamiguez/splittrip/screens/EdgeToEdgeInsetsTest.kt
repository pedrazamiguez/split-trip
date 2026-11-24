package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowInsets as WindowInsetsOverride
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormSubmitButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.BRANDED_LOADING_SCREEN_TEST_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.MainAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.features.authentication.R as AuthR
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.ForgotPasswordScreen
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.LoginScreen
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.RegisterScreen
import es.pedrazamiguez.splittrip.features.onboarding.R as OnboardingR
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.OnboardingScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.ReconciliationScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.OnboardingUiState
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.ReconciliationUiState
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.module

private const val ROOT_TAG = "edge_to_edge_root"
private const val CONTENT_TAG = "edge_to_edge_content"
private const val SUBMIT_TAG = "edge_to_edge_submit"
private const val BOUNDS_TOLERANCE_DP = 0.5f

@RunWith(AndroidJUnit4::class)
class EdgeToEdgeInsetsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        stopKoin()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun buildInsets(
        cutoutLeftPx: Int = 0,
        cutoutTopPx: Int = 0,
        cutoutRightPx: Int = 0,
        cutoutBottomPx: Int = 0,
        navLeftPx: Int = 0,
        navTopPx: Int = 0,
        navRightPx: Int = 0,
        navBottomPx: Int = 0
    ): WindowInsetsCompat = WindowInsetsCompat.Builder()
        .setInsets(
            WindowInsetsCompat.Type.displayCutout(),
            AndroidXInsets.of(cutoutLeftPx, cutoutTopPx, cutoutRightPx, cutoutBottomPx)
        )
        .setInsets(
            WindowInsetsCompat.Type.navigationBars(),
            AndroidXInsets.of(navLeftPx, navTopPx, navRightPx, navBottomPx)
        )
        .build()

    @Test
    fun featureScaffold_standalone_appliesHorizontalCutoutAndNavBarInsetsWithoutDoublePaddingChild() {
        val cutoutLeftPx = 48
        val navRightPx = 96
        val navBottomPx = 72
        val simulatedInsets = buildInsets(
            cutoutLeftPx = cutoutLeftPx,
            navRightPx = navRightPx,
            navBottomPx = navBottomPx
        )
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            KoinApplication(application = { modules(module { single<List<ScreenUiProvider>> { emptyList() } }) }) {
                SplitTripTheme {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                            FeatureScaffold(currentRoute = "test_standalone") {
                                Column(modifier = Modifier.fillMaxSize().testTag(CONTENT_TAG)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    FormSubmitButton(
                                        label = "Save",
                                        isEnabled = true,
                                        isLoading = false,
                                        onSubmit = {},
                                        modifier = Modifier.testTag(SUBMIT_TAG)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val contentBounds = composeRule.onNodeWithTag(CONTENT_TAG).getUnclippedBoundsInRoot()
        val submitBounds = composeRule.onNodeWithTag(SUBMIT_TAG).getUnclippedBoundsInRoot()

        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density
        val expectedBottomDp = navBottomPx / density

        assertTrue(contentBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(contentBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
        assertTrue(
            "Submit button bottom (${submitBounds.bottom.value}) should clear nav bar once without double-padding",
            submitBounds.bottom.value <= rootBounds.bottom.value - expectedBottomDp + BOUNDS_TOLERANCE_DP &&
                submitBounds.bottom.value > rootBounds.bottom.value - expectedBottomDp * 2f + BOUNDS_TOLERANCE_DP
        )
    }

    @Test
    fun featureScaffold_tabHosted_appliesHorizontalSafeDrawingInsetsWhenUsedStandalone() {
        val cutoutLeftPx = 48
        val simulatedInsets = buildInsets(cutoutLeftPx = cutoutLeftPx)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    CompositionLocalProvider(LocalTopPadding provides 56.dp) {
                        FeatureScaffold(currentRoute = "tab_hosted") {
                            Box(modifier = Modifier.fillMaxSize().testTag(CONTENT_TAG))
                        }
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val contentBounds = composeRule.onNodeWithTag(CONTENT_TAG).getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        assertTrue(contentBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
    }

    @Test
    fun floatingNavigationBar_applyWindowInsetsTrue_respectsHorizontalCutoutAndNavBarInsets() {
        val cutoutLeftPx = 48
        val navRightPx = 96
        val navBottomPx = 48
        val simulatedInsets = buildInsets(
            cutoutLeftPx = cutoutLeftPx,
            navRightPx = navRightPx,
            navBottomPx = navBottomPx
        )
        val tab = object : FloatingNavTab {
            override val id: String = "groups"

            @Composable
            override fun Icon(isSelected: Boolean, tint: Color) {
                Icon(imageVector = TablerIcons.Outline.UsersGroup, contentDescription = null, tint = tint)
            }

            @Composable
            override fun getLabel(): String = "Groups"
        }
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        FloatingNavigationBar(
                            selectedId = "groups",
                            items = listOf(tab),
                            mainAction = MainAction(
                                icon = TablerIcons.Outline.Plus,
                                contentDescription = "Add",
                                onClick = {}
                            ),
                            applyWindowInsets = true
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val tabBounds = composeRule.onNodeWithText("Groups").getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density
        val expectedBottomDp = navBottomPx / density

        assertTrue(tabBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(tabBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
        assertTrue(tabBounds.bottom.value <= rootBounds.bottom.value - expectedBottomDp + BOUNDS_TOLERANCE_DP)
    }

    @Test
    fun loginScreen_landscapeSafeDrawingInsets_keepsFormInsideCutoutBounds() {
        val cutoutLeftPx = 60
        val navRightPx = 96
        val simulatedInsets = buildInsets(cutoutLeftPx = cutoutLeftPx, navRightPx = navRightPx)
        val googleButtonText = context.getString(AuthR.string.login_google_button)
        val emailButtonText = context.getString(AuthR.string.login_continue_with_email)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        LoginScreen(uiState = AuthenticationUiState())
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val googleBounds = composeRule.onNodeWithText(googleButtonText).getUnclippedBoundsInRoot()
        val emailBounds = composeRule.onNodeWithText(emailButtonText).getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density

        assertTrue(googleBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(googleBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
        assertTrue(emailBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(emailBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
    }

    @Test
    fun registerScreen_landscapeSafeDrawingInsets_keepsFormInsideCutoutBounds() {
        val cutoutLeftPx = 60
        val navRightPx = 96
        val simulatedInsets = buildInsets(cutoutLeftPx = cutoutLeftPx, navRightPx = navRightPx)
        val submitText = context.getString(AuthR.string.register_submit_button)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        RegisterScreen(uiState = RegisterUiState())
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val submitBounds = composeRule.onNodeWithText(submitText).getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density

        assertTrue(submitBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(submitBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
    }

    @Test
    fun forgotPasswordScreen_landscapeSafeDrawingInsets_keepsContentInsideCutoutBounds() {
        val cutoutLeftPx = 60
        val navRightPx = 96
        val simulatedInsets = buildInsets(cutoutLeftPx = cutoutLeftPx, navRightPx = navRightPx)
        val submitText = context.getString(AuthR.string.forgot_password_button)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        ForgotPasswordScreen(uiState = ForgotPasswordUiState())
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val submitBounds = composeRule.onNodeWithText(submitText).getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density

        assertTrue(submitBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(submitBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
    }

    @Test
    fun onboardingScreen_landscapeSafeDrawingInsets_keepsControlsInsideCutoutBounds() {
        val cutoutLeftPx = 60
        val navRightPx = 96
        val navBottomPx = 48
        val simulatedInsets = buildInsets(
            cutoutLeftPx = cutoutLeftPx,
            navRightPx = navRightPx,
            navBottomPx = navBottomPx
        )
        val skipText = context.getString(OnboardingR.string.onboarding_skip_button)
        val nextText = context.getString(OnboardingR.string.onboarding_next_button)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        OnboardingScreen(uiState = OnboardingUiState())
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val skipBounds = composeRule.onNodeWithText(skipText).getUnclippedBoundsInRoot()
        val nextBounds = composeRule.onNodeWithText(nextText).getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density
        val expectedBottomDp = navBottomPx / density

        assertTrue(skipBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(skipBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
        assertTrue(nextBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(nextBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
        assertTrue(nextBounds.bottom.value <= rootBounds.bottom.value - expectedBottomDp + BOUNDS_TOLERANCE_DP)
    }

    @Test
    fun reconciliationScreen_migratingAndIdleStates_respectSafeDrawingInsets() {
        val cutoutLeftPx = 60
        val navRightPx = 96
        val simulatedInsets = buildInsets(cutoutLeftPx = cutoutLeftPx, navRightPx = navRightPx)
        val migrateButtonText = context.getString(OnboardingR.string.reconciliation_migrate_button)
        var density = 1f

        composeRule.setContent {
            density = LocalDensity.current.density
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        ReconciliationScreen(
                            uiState = ReconciliationUiState.Migrating,
                            email = "user@example.com",
                            onMigrateClick = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val rootBounds = composeRule.onNodeWithTag(ROOT_TAG).getUnclippedBoundsInRoot()
        val loadingBounds = composeRule.onNodeWithTag(BRANDED_LOADING_SCREEN_TEST_TAG)
            .getUnclippedBoundsInRoot()
        val expectedLeftDp = cutoutLeftPx / density
        val expectedRightDp = navRightPx / density

        assertTrue(loadingBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(loadingBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)

        composeRule.setContent {
            SplitTripTheme {
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.WindowInsetsOverride(simulatedInsets)
                ) {
                    Box(modifier = Modifier.fillMaxSize().testTag(ROOT_TAG)) {
                        ReconciliationScreen(
                            uiState = ReconciliationUiState.WaitingForYou,
                            email = "user@example.com",
                            onMigrateClick = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        val ctaBounds = composeRule.onNodeWithText(migrateButtonText).getUnclippedBoundsInRoot()
        assertTrue(ctaBounds.left.value >= expectedLeftDp - BOUNDS_TOLERANCE_DP)
        assertTrue(ctaBounds.right.value <= rootBounds.right.value - expectedRightDp + BOUNDS_TOLERANCE_DP)
    }
}
